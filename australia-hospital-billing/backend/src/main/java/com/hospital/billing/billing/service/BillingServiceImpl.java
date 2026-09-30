package com.hospital.billing.billing.service;

import org.springframework.context.ApplicationEventPublisher;
import com.hospital.billing.pharmacy.service.PharmacyService;
import com.hospital.billing.common.event.BillFinalizedEvent;
import com.hospital.billing.adjudication.AdjudicationEngine;
import com.hospital.billing.adjudication.AdjudicationLine;
import com.hospital.billing.adjudication.AdjudicationResult;
import com.hospital.billing.adjudication.CoverageProfile;
import com.hospital.billing.adjudication.LineOutcome;
import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.domain.AdmissionStatus;
import com.hospital.billing.admission.service.AdmissionReadOperations;
import com.hospital.billing.billing.charge.ChargeCollector;
import com.hospital.billing.billing.charge.ChargeLine;
import com.hospital.billing.billing.charge.GstCalculator;
import com.hospital.billing.billing.domain.BillItem;
import com.hospital.billing.billing.domain.BillStatus;
import com.hospital.billing.billing.domain.HospitalBill;
import com.hospital.billing.billing.domain.HospitalBillRepository;
import com.hospital.billing.billing.domain.PaidBy;
import com.hospital.billing.billing.web.BillingDtos.BillResponse;
import com.hospital.billing.billing.web.BillingDtos.BillSummary;
import com.hospital.billing.billing.web.BillingDtos.CoverageSummary;
import com.hospital.billing.billing.web.BillingDtos.ManualItemRequest;
import com.hospital.billing.claim.event.ClaimEvent;
import com.hospital.billing.claim.event.ClaimEventPublisher;
import com.hospital.billing.claim.event.ClaimEventType;
import com.hospital.billing.common.domain.DocumentNumberGenerator;
import com.hospital.billing.common.domain.DocumentType;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.DuplicateResourceException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Billing pipeline, decomposed by responsibility:
 * <pre>
 *  ChargeCollectors (room, doctor, lab, pharmacy) -> GstCalculator -> HospitalBill items
 *        -> CoverageResolver -> AdjudicationEngine (Medicare / private fund strategies)
 *        -> per-line payer split -> bill totals -> ClaimEventPublisher (notifications)
 * </pre>
 * This class only orchestrates; each step lives in its own class (SRP).
 * The tenant is never passed around: Hibernate filters by TenantContext automatically,
 * and TenantContext is read explicitly only to stamp outgoing events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingServiceImpl implements BillingService {

    private final HospitalBillRepository billRepository;
    private final AdmissionReadOperations admissionReads;
    private final List<ChargeCollector> chargeCollectors;
    private final GstCalculator gstCalculator;
    private final CoverageResolver coverageResolver;
    private final AdjudicationEngine adjudicationEngine;
    private final ClaimEventPublisher eventPublisher;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final PharmacyService pharmacyService;
    private final ApplicationEventPublisher applicationEvents;
    private final Clock clock;

    @Override
    @Transactional
    public BillResponse generateBill(Long admissionId) {
        Admission admission = admissionReads.requireAdmission(admissionId);
        if (admission.getStatus() == AdmissionStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot bill a cancelled admission");
        }
        if (billRepository.existsByAdmissionIdAndStatusNot(admissionId, BillStatus.CANCELLED)) {
            throw new DuplicateResourceException(
                    "Admission %s already has an active bill".formatted(admission.getAdmissionNo()));
        }

        HospitalBill bill = new HospitalBill();
        bill.setBillNo(documentNumberGenerator.next(DocumentType.BILL));
        bill.setAdmission(admission);
        bill.setBillDate(LocalDateTime.now(clock));
        collectAutomaticCharges(bill, admission);
        if (admission.getStatus() == AdmissionStatus.DISCHARGED) {
            bill.lockCharges(admission.getDischargeDate());     // stay is complete: freeze the charges
        }
        billRepository.saveAndFlush(bill);          // item ids are needed by the engine

        AdjudicationResult result = adjudicate(bill);
        log.info("Generated bill {} for admission {} with {} items, net {}", bill.getBillNo(),
                admission.getAdmissionNo(), bill.getItems().size(), bill.getNetAmount());
        return toResponse(bill, result.notes());
    }

    @Override
    @Transactional
    public BillResponse recalculate(Long billId) {
        HospitalBill bill = requireBill(billId);
        bill.requireDraft();
        refreshAutomaticCharges(bill);
        AdjudicationResult result = adjudicate(bill);
        return toResponse(bill, result.notes());
    }

    @Override
    @Transactional
    public BillResponse addManualItem(Long billId, ManualItemRequest request) {
        HospitalBill bill = requireBill(billId);
        BigDecimal discount = request.discountAmount() == null ? Money.ZERO : request.discountAmount();
        ChargeLine line = new ChargeLine(request.itemType(), null, request.description().trim(), request.mbsItemNo(),
                request.mbsScheduleFee(), request.quantity(), request.unitPrice(), null);
        BillItem item = toItem(line, discount);
        bill.addItem(item);
        billRepository.saveAndFlush(bill);
        AdjudicationResult result = adjudicate(bill);
        return toResponse(bill, result.notes());
    }

    @Override
    @Transactional
    public BillResponse removeItem(Long billId, Long itemId) {
        HospitalBill bill = requireBill(billId);
        BillItem item = bill.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Bill item", itemId));
        bill.removeItem(item);
        billRepository.flush();
        AdjudicationResult result = adjudicate(bill);
        return toResponse(bill, result.notes());
    }

    @Override
    @Transactional
    public BillResponse finalizeBill(Long billId) {
        HospitalBill bill = requireBill(billId);
        if (bill.getAdmission().isActive()) {
            throw new BusinessRuleException("Discharge the patient before finalising the bill");
        }
        // Bring accommodation days etc. up to the discharge date before locking the bill.
        refreshAutomaticCharges(bill);
        AdjudicationResult result = adjudicate(bill);
        bill.finalizeBill();
        // After commit the claims module splits the bill into Medicare / fund claims automatically.
        applicationEvents.publishEvent(new BillFinalizedEvent(bill.getId(), bill.getBillNo()));

        eventPublisher.publish(ClaimEvent.of(ClaimEventType.NOTIFICATION, TenantContext.requireCurrentTenant(),
                null, bill.getBillNo(), bill.getPatientPayableAmount(),
                "Your hospital bill %s is ready. Estimated out-of-pocket amount: $%s"
                        .formatted(bill.getBillNo(), bill.getPatientPayableAmount())));
        log.info("Finalised bill {}: net {}, Medicare {}, fund {}, patient {}", bill.getBillNo(),
                bill.getNetAmount(), bill.getMedicareAmount(), bill.getInsuranceAmount(),
                bill.getPatientPayableAmount());
        return toResponse(bill, result.notes());
    }

    @Override
    @Transactional
    public BillResponse prepareDischargeBill(Long admissionId, LocalDateTime dischargedAt) {
        var existing = billRepository.findFirstByAdmissionIdAndStatusNotOrderByIdDesc(admissionId, BillStatus.CANCELLED);
        if (existing.isEmpty()) {
            return generateBill(admissionId);        // locks because the admission is now DISCHARGED
        }
        HospitalBill bill = existing.get();
        if (!bill.isDraft()) {
            return toResponse(bill, List.of());
        }
        refreshAutomaticCharges(bill);               // re-cost room days up to the discharge time
        bill.lockCharges(dischargedAt);
        AdjudicationResult result = adjudicate(bill);
        log.info("Locked {} charge lines on bill {} at discharge", bill.getItems().size(), bill.getBillNo());
        return toResponse(bill, result.notes());
    }

    @Override
    @Transactional
    public void syncDraftBill(Long admissionId) {
        billRepository.findFirstByAdmissionIdAndStatusNotOrderByIdDesc(admissionId, BillStatus.CANCELLED)
                .filter(HospitalBill::isDraft)
                .filter(bill -> !bill.isChargesLocked())
                .ifPresent(bill -> {
                    refreshAutomaticCharges(bill);
                    adjudicate(bill);
                    log.debug("Re-synced draft bill {} after a clinical change", bill.getBillNo());
                });
    }

    @Override
    @Transactional
    public BillResponse cancel(Long billId) {
        HospitalBill bill = requireBill(billId);
        bill.cancel();
        return toResponse(bill, List.of());
    }

    @Override
    public BillResponse get(Long billId) {
        return toResponse(requireBill(billId), List.of());
    }

    @Override
    public PageResponse<BillSummary> search(BillStatus status, Long admissionId, Pageable pageable) {
        Page<HospitalBill> page;
        if (admissionId != null) {
            page = billRepository.findByAdmissionId(admissionId, pageable);
        } else if (status != null) {
            page = billRepository.findByStatus(status, pageable);
        } else {
            page = billRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(BillSummary::from));
    }

    @Override
    public HospitalBill requireBill(Long billId) {
        return billRepository.findWithItemsById(billId)
                .orElseThrow(() -> new EntityNotFoundException("Bill", billId));
    }

    @Override
    @Transactional
    public void shiftRejectedBenefitToPatient(Long billId, PaidBy payer, Map<Long, BigDecimal> rejectedByBillItem) {
        HospitalBill bill = requireBill(billId);
        bill.getItems().forEach(item -> {
            BigDecimal rejected = rejectedByBillItem.get(item.getId());
            if (rejected != null && rejected.signum() > 0) {
                item.shiftToPatient(payer, rejected);
            }
        });
        bill.recalculateTotals();
        log.info("Moved rejected {} benefit to patient on bill {}; patient now owes {}", payer, bill.getBillNo(),
                bill.getPatientPayableAmount());
    }

    // ------------------------------------------------------------------ helpers

    private void refreshAutomaticCharges(HospitalBill bill) {
        if (bill.isChargesLocked()) {
            return;                                  // frozen at discharge: only re-adjudicate
        }
        List<BillItem> automatic = bill.getItems().stream().filter(i -> !i.isManual()).toList();
        automatic.forEach(bill::removeItem);
        billRepository.flush();
        collectAutomaticCharges(bill, bill.getAdmission());
        billRepository.saveAndFlush(bill);
    }

    private void collectAutomaticCharges(HospitalBill bill, Admission admission) {
        LocalDateTime asOf = admission.getDischargeDate() != null ? admission.getDischargeDate() : LocalDateTime.now(clock);
        for (ChargeCollector collector : chargeCollectors) {
            collector.collect(admission, asOf).forEach(line -> bill.addItem(toItem(line, Money.ZERO)));
        }
    }

    private BillItem toItem(ChargeLine line, BigDecimal discount) {
        BigDecimal gross = Money.of(line.quantity().multiply(line.unitPrice()));
        BigDecimal discountAmount = Money.of(discount);
        if (discountAmount.compareTo(gross) > 0) {
            throw new BusinessRuleException("Discount cannot exceed the line amount");
        }
        BigDecimal taxable = Money.of(gross.subtract(discountAmount));
        BigDecimal gst = gstCalculator.gst(taxable, gstCalculator.rateFor(line.itemType(), line.gstRate()));

        BillItem item = new BillItem();
        item.setItemType(line.itemType());
        item.setReferenceId(line.referenceId());
        item.setDescription(line.description());
        item.setMbsItemNo(line.mbsItemNo());
        item.setMbsScheduleFee(line.mbsScheduleFee() == null ? null : Money.of(line.mbsScheduleFee()));
        item.setQuantity(line.quantity().setScale(2, RoundingMode.HALF_UP));
        item.setUnitPrice(Money.of(line.unitPrice()));
        item.setGrossAmount(gross);
        item.setDiscountAmount(discountAmount);
        item.setGstAmount(gst);
        item.setNetAmount(Money.of(taxable.add(gst)));
        item.applySplit(Money.ZERO, Money.ZERO, item.getNetAmount());
        return item;
    }

    private static List<AdjudicationLine> linesOf(HospitalBill bill) {
        List<AdjudicationLine> lines = new ArrayList<>();
        for (BillItem item : bill.getItems()) {
            BigDecimal scheduleTotal = item.getMbsScheduleFee() == null
                    ? null
                    : Money.of(item.getMbsScheduleFee().multiply(item.getQuantity()));
            lines.add(new AdjudicationLine(item.getId(), item.getItemType(), item.getNetAmount(), scheduleTotal));
        }
        return lines;
    }

    /** Runs the AdjudicationEngine and writes the payer split onto every line and the totals. */
    private AdjudicationResult adjudicate(HospitalBill bill) {
        CoverageProfile coverage = coverageResolver.resolve(bill.getAdmission());
        AdjudicationResult result = adjudicationEngine.adjudicate(linesOf(bill), coverage);
        Map<Long, LineOutcome> outcomes = result.byBillItemId();
        bill.getItems().forEach(item -> {
            LineOutcome outcome = outcomes.get(item.getId());
            item.applySplit(outcome.medicare(), outcome.privateFund(), outcome.patientGap());
        });
        bill.recalculateTotals();
        return result;
    }

    private BillResponse toResponse(HospitalBill bill, List<String> notes) {
        if (notes.isEmpty() && !bill.getItems().isEmpty()) {
            notes = adjudicationEngine.adjudicate(linesOf(bill), coverageResolver.resolve(bill.getAdmission())).notes();
        }
        CoverageSummary coverage = CoverageSummary.from(coverageResolver.resolve(bill.getAdmission()));
        return BillResponse.from(bill, coverage, notes, warnings(bill));
    }

    /** Things the billing officer should know before finalising (charges not yet on the bill). */
    private List<String> warnings(HospitalBill bill) {
        List<String> warnings = new ArrayList<>();
        Admission admission = bill.getAdmission();
        if (admission.isActive()) {
            warnings.add("Interim bill: the patient is still admitted; the stay is re-costed and locked at discharge.");
        }
        long awaiting = pharmacyService.countAwaitingDispense(admission.getId());
        if (awaiting > 0) {
            warnings.add("%d prescription(s) are not dispensed yet and are therefore not billed.".formatted(awaiting));
        }
        return warnings;
    }
}
