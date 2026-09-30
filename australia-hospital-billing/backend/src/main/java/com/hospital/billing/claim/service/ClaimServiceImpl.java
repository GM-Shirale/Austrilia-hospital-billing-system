package com.hospital.billing.claim.service;

import com.hospital.billing.adjudication.CoverageProfile;
import com.hospital.billing.billing.domain.BillItem;
import com.hospital.billing.billing.domain.HospitalBill;
import com.hospital.billing.billing.service.BillingService;
import com.hospital.billing.billing.service.CoverageResolver;
import com.hospital.billing.claim.domain.ClaimItem;
import com.hospital.billing.claim.domain.ClaimStatus;
import com.hospital.billing.claim.domain.ExplanationOfBenefitRepository;
import com.hospital.billing.claim.domain.InsuranceClaim;
import com.hospital.billing.claim.domain.InsuranceClaimRepository;
import com.hospital.billing.claim.event.ClaimEvent;
import com.hospital.billing.claim.event.ClaimEventPublisher;
import com.hospital.billing.claim.event.ClaimEventType;
import com.hospital.billing.claim.web.ClaimDtos.ClaimItemResponse;
import com.hospital.billing.claim.web.ClaimDtos.ClaimResponse;
import com.hospital.billing.claim.web.ClaimDtos.ClaimSummary;
import com.hospital.billing.claim.web.ClaimDtos.EobResponse;
import com.hospital.billing.claim.web.ClaimDtos.StatusHistoryResponse;
import com.hospital.billing.common.domain.DocumentNumberGenerator;
import com.hospital.billing.common.domain.DocumentType;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.ClaimValidationException;
import com.hospital.billing.common.exception.DuplicateClaimException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.insurance.domain.InsuranceCompany;
import com.hospital.billing.insurance.domain.InsurancePolicy;
import com.hospital.billing.insurance.domain.PayerType;
import com.hospital.billing.insurance.service.InsuranceService;
import com.hospital.billing.security.CurrentUser;
import com.hospital.billing.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimServiceImpl implements ClaimService {

    private final InsuranceClaimRepository claimRepository;
    private final ExplanationOfBenefitRepository eobRepository;
    private final BillingService billingService;
    private final CoverageResolver coverageResolver;
    private final InsuranceService insuranceService;
    private final ClaimValidator claimValidator;
    private final ClaimEventPublisher eventPublisher;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public List<ClaimSummary> createClaimsForBill(Long billId) {
        HospitalBill bill = billingService.requireBill(billId);
        bill.requirePayable();

        List<InsuranceClaim> created = new ArrayList<>();
        List<String> alreadyClaimed = new ArrayList<>();

        if (bill.getMedicareAmount().signum() > 0) {
            if (claimRepository.existsByBillIdAndPayerType(billId, PayerType.MEDICARE)) {
                alreadyClaimed.add("Medicare");
            } else {
                InsuranceCompany medicare = insuranceService.findMedicare()
                        .orElseThrow(() -> new BusinessRuleException("Medicare is not configured as a payer for this hospital"));
                created.add(newClaim(bill, PayerType.MEDICARE, medicare, null, BillItem::getMedicareBenefit));
            }
        }
        if (bill.getInsuranceAmount().signum() > 0) {
            if (claimRepository.existsByBillIdAndPayerType(billId, PayerType.PRIVATE_FUND)) {
                alreadyClaimed.add("private fund");
            } else {
                CoverageProfile coverage = coverageResolver.resolve(bill.getAdmission());
                if (!coverage.hasPrivateCover()) {
                    throw new BusinessRuleException("The patient no longer has active private cover for this admission");
                }
                InsurancePolicy policy = insuranceService.requirePolicy(coverage.privateCover().policyId());
                created.add(newClaim(bill, PayerType.PRIVATE_FUND, policy.getCompany(), policy,
                        BillItem::getInsuranceCoveredAmount));
            }
        }

        if (created.isEmpty() && !alreadyClaimed.isEmpty()) {
            throw new DuplicateClaimException("Claims already exist for bill %s (%s)"
                    .formatted(bill.getBillNo(), String.join(", ", alreadyClaimed)));
        }
        if (created.isEmpty()) {
            throw new BusinessRuleException("Nothing to claim: the bill has no Medicare or insurance benefit");
        }
        return created.stream().map(ClaimSummary::from).toList();
    }

    @Override
    @Transactional
    public ClaimResponse validate(Long claimId) {
        InsuranceClaim claim = requireClaim(claimId);
        List<String> violations = claimValidator.validate(claim);
        if (!violations.isEmpty()) {
            throw new ClaimValidationException("Claim %s failed %d pre-submission check(s)"
                    .formatted(claim.getClaimNo(), violations.size()), violations);
        }
        claim.transitionTo(ClaimStatus.VALIDATED, "All pre-submission checks passed", now());
        return toResponse(claim);
    }

    @Override
    @Transactional
    public ClaimResponse submit(Long claimId) {
        InsuranceClaim claim = requireClaim(claimId);
        claim.transitionTo(ClaimStatus.SUBMISSION_QUEUED,
                "Queued for lodgement with %s by %s".formatted(claim.getCompany().getCompanyName(),
                        CurrentUser.usernameOrSystem()), now());
        eventPublisher.publish(ClaimEvent.of(ClaimEventType.CLAIM_SUBMITTED, TenantContext.requireCurrentTenant(),
                claim.getId(), claim.getClaimNo(), claim.getClaimedAmount(), "Claim queued for submission"));
        log.info("Claim {} queued for submission to {}", claim.getClaimNo(), claim.getCompany().getCompanyName());
        return toResponse(claim);
    }

    @Override
    @Transactional
    public ClaimResponse close(Long claimId, String note) {
        InsuranceClaim claim = requireClaim(claimId);
        String reason = note == null || note.isBlank() ? "Closed by " + CurrentUser.usernameOrSystem() : note.trim();
        claim.transitionTo(ClaimStatus.CLOSED, reason, now());
        return toResponse(claim);
    }

    @Override
    public ClaimResponse get(Long claimId) {
        return toResponse(requireClaim(claimId));
    }

    @Override
    public PageResponse<ClaimSummary> search(ClaimStatus status, Pageable pageable) {
        Page<InsuranceClaim> page = status == null
                ? claimRepository.findAll(pageable)
                : claimRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(ClaimSummary::from));
    }

    @Override
    public List<ClaimSummary> claimsForBill(Long billId) {
        return claimRepository.findByBillIdOrderByIdAsc(billId).stream().map(ClaimSummary::from).toList();
    }

    // ------------------------------------------------------------------ helpers

    private InsuranceClaim newClaim(HospitalBill bill, PayerType payerType, InsuranceCompany payer,
                                    InsurancePolicy policy, Function<BillItem, BigDecimal> benefitOf) {
        InsuranceClaim claim = new InsuranceClaim();
        claim.setClaimNo(documentNumberGenerator.next(DocumentType.CLAIM));
        claim.setBill(bill);
        claim.setAdmission(bill.getAdmission());
        claim.setCompany(payer);
        claim.setPolicy(policy);
        claim.setPayerType(payerType);
        claim.setClaimDate(now());
        for (BillItem billItem : bill.getItems()) {
            BigDecimal benefit = benefitOf.apply(billItem);
            if (benefit.signum() > 0) {
                ClaimItem item = new ClaimItem();
                item.setBillItem(billItem);
                item.setClaimedAmount(benefit);
                claim.addItem(item);
            }
        }
        claim.recordCreation(now());
        InsuranceClaim saved = claimRepository.save(claim);
        log.info("Created {} claim {} for bill {} claiming {}", payerType, saved.getClaimNo(), bill.getBillNo(),
                saved.getClaimedAmount());
        return saved;
    }

    private InsuranceClaim requireClaim(Long claimId) {
        return claimRepository.findWithItemsById(claimId).orElseThrow(() -> new EntityNotFoundException("Claim", claimId));
    }

    private ClaimResponse toResponse(InsuranceClaim claim) {
        List<ClaimStatus> next = Arrays.stream(ClaimStatus.values())
                .filter(s -> claim.getStatus().canTransitionTo(s))
                .toList();
        EobResponse eob = eobRepository.findByClaimId(claim.getId()).map(EobResponse::from).orElse(null);
        List<StatusHistoryResponse> history = claim.getHistory().stream().map(StatusHistoryResponse::from).toList();
        return new ClaimResponse(ClaimSummary.from(claim),
                claim.getPolicy() == null ? null : claim.getPolicy().getPolicyNo(),
                claim.getPayerReference(), claim.getRejectionReason(), claim.getSubmittedAt(),
                claim.getAdjudicatedAt(), claim.getPaidAt(), next,
                claim.getItems().stream().map(ClaimItemResponse::from).toList(), history, eob);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
