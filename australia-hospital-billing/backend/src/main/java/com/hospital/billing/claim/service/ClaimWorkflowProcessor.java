package com.hospital.billing.claim.service;

import com.hospital.billing.billing.domain.PaidBy;
import com.hospital.billing.billing.domain.Payment;
import com.hospital.billing.billing.service.BillingService;
import com.hospital.billing.billing.service.PaymentService;
import com.hospital.billing.claim.domain.ClaimItem;
import com.hospital.billing.claim.domain.ClaimStatus;
import com.hospital.billing.claim.domain.ExplanationOfBenefit;
import com.hospital.billing.claim.domain.InsuranceClaim;
import com.hospital.billing.claim.domain.InsuranceClaimRepository;
import com.hospital.billing.claim.event.ClaimEvent;
import com.hospital.billing.claim.event.ClaimEventPublisher;
import com.hospital.billing.claim.event.ClaimEventType;
import com.hospital.billing.claim.integration.ClaimSubmission;
import com.hospital.billing.claim.integration.InsuranceClaimAdapter;
import com.hospital.billing.claim.integration.PayerCommunicationException;
import com.hospital.billing.claim.integration.PayerDecision;
import com.hospital.billing.claim.integration.RetryExecutor;
import com.hospital.billing.claim.integration.SubmissionReceipt;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.insurance.domain.InsurancePolicy;
import com.hospital.billing.insurance.domain.PayerType;
import com.hospital.billing.insurance.service.InsuranceService;
import com.hospital.billing.patient.domain.Patient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Asynchronous steps of the claim state machine, driven by events. Every handler:
 * <ul>
 *   <li>is called with TenantContext already bound (by {@code ClaimEventRouter});</li>
 *   <li>is idempotent: it checks the expected current status first, because Kafka delivers
 *       at-least-once and a duplicate must not pay a bill twice;</li>
 *   <li>never holds a DB transaction open while waiting on the payer: read in one short
 *       transaction, call the adapter, write the outcome in a second short transaction.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimWorkflowProcessor {

    private final InsuranceClaimRepository claimRepository;
    private final InsuranceClaimAdapter claimAdapter;
    private final RetryExecutor retryExecutor;
    private final BillingService billingService;
    private final PaymentService paymentService;
    private final InsuranceService insuranceService;
    private final EobService eobService;
    private final ClaimEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /** claim.submitted: lodge with the payer and ingest the payer's decision. */
    public void handleSubmitted(ClaimEvent event) {
        Optional<ClaimSubmission> maybeSubmission = transactionTemplate.execute(status ->
                claimRepository.findWithItemsById(event.claimId())
                        .filter(claim -> {
                            boolean expected = claim.getStatus() == ClaimStatus.SUBMISSION_QUEUED;
                            if (!expected) {
                                log.info("Ignoring duplicate submission event for {} (status {})",
                                        claim.getClaimNo(), claim.getStatus());
                            }
                            return expected;
                        })
                        .map(this::toSubmission));
        if (maybeSubmission == null || maybeSubmission.isEmpty()) {
            return;
        }
        ClaimSubmission submission = maybeSubmission.get();

        // No transaction is open while we talk to the payer (with retries and back-off).
        PayerOutcome outcome;
        try {
            outcome = callPayer(submission);
        } catch (PayerCommunicationException ex) {
            transactionTemplate.executeWithoutResult(status -> rejectForCommunicationFailure(event, ex));
            return;
        }
        transactionTemplate.executeWithoutResult(status -> applyDecision(event, outcome.receipt(), outcome.decision()));
    }

    private record PayerOutcome(SubmissionReceipt receipt, PayerDecision decision) {
    }

    private PayerOutcome callPayer(ClaimSubmission submission) {
        SubmissionReceipt receipt = retryExecutor.execute("Submit claim " + submission.claimNo(),
                () -> claimAdapter.submit(submission));
        PayerDecision decision = retryExecutor.execute("Fetch decision for " + submission.claimNo(),
                () -> claimAdapter.fetchDecision(receipt.payerReference(), submission));
        return new PayerOutcome(receipt, decision);
    }

    /** claim.adjudicated: the payer remits the approved amount (simulated ERA in dev). */
    public void handleAdjudicated(ClaimEvent event) {
        transactionTemplate.executeWithoutResult(status -> {
            InsuranceClaim claim = claimRepository.findById(event.claimId()).orElse(null);
            if (claim == null || claim.getStatus() != ClaimStatus.ACCEPTED) {
                return;
            }
            // In production this event comes from the payer's Electronic Remittance Advice feed.
            eventPublisher.publish(ClaimEvent.of(ClaimEventType.REMITTANCE_POSTED, event.tenantId(), claim.getId(),
                    claim.getClaimNo(), claim.getApprovedAmount(), "Remittance " + claim.getPayerReference()));
        });
    }

    /** remittance.posted: post payment to the bill, update accumulator, issue EOB, reconcile. */
    public void handleRemittance(ClaimEvent event) {
        transactionTemplate.executeWithoutResult(status -> {
            InsuranceClaim claim = claimRepository.findWithItemsById(event.claimId()).orElse(null);
            if (claim == null || claim.getStatus() != ClaimStatus.ACCEPTED) {
                log.info("Ignoring remittance for claim {}: not in ACCEPTED state", event.reference());
                return;
            }
            LocalDateTime now = LocalDateTime.now(clock);
            BigDecimal amount = Money.of(event.amount());
            Payment payment = paymentService.recordRemittance(claim.getBill().getId(),
                    PaidBy.from(claim.getPayerType()), amount, claim.getPayerReference());
            claim.setPaidAmount(payment.getAmount());
            claim.setPaidAt(now);
            claim.transitionTo(ClaimStatus.PAID, "Remittance of $%s received (payment %s)"
                    .formatted(payment.getAmount(), payment.getPaymentNo()), now);

            if (claim.getPayerType() == PayerType.PRIVATE_FUND && claim.getPolicy() != null) {
                insuranceService.recordBenefitPaid(claim.getPolicy().getId(), payment.getAmount());
            }
            ExplanationOfBenefit eob = eobService.generate(claim);
            claim.transitionTo(ClaimStatus.RECONCILED, "Payment reconciled to bill %s; EOB %s issued"
                    .formatted(claim.getBill().getBillNo(), eob.getEobNo()), now);

            eventPublisher.publish(ClaimEvent.of(ClaimEventType.NOTIFICATION, event.tenantId(), claim.getId(),
                    claim.getClaimNo(), payment.getAmount(), eob.getSummary()));
            log.info("Claim {} paid {} and reconciled", claim.getClaimNo(), payment.getAmount());
        });
    }

    // ------------------------------------------------------------------ steps

    private void applyDecision(ClaimEvent event, SubmissionReceipt receipt, PayerDecision decision) {
        InsuranceClaim claim = claimRepository.findWithItemsById(event.claimId()).orElseThrow();
        if (claim.getStatus() != ClaimStatus.SUBMISSION_QUEUED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        claim.setPayerReference(receipt.payerReference());
        claim.setSubmittedAt(now);
        claim.transitionTo(ClaimStatus.SUBMITTED, "Lodged with %s, reference %s"
                .formatted(claim.getCompany().getCompanyName(), receipt.payerReference()), now);
        claim.transitionTo(ClaimStatus.ACKNOWLEDGED, "Payer acknowledged receipt", now);

        Map<Long, PayerDecision.LineDecision> byItem = decision.lines().stream()
                .collect(Collectors.toMap(PayerDecision.LineDecision::claimItemId, Function.identity()));
        BigDecimal approved = Money.ZERO;
        BigDecimal rejected = Money.ZERO;
        Map<Long, BigDecimal> rejectedByBillItem = new HashMap<>();
        String firstReason = null;
        for (ClaimItem item : claim.getItems()) {
            PayerDecision.LineDecision line = byItem.get(item.getId());
            BigDecimal lineApproved = line == null ? Money.ZERO
                    : Money.min(Money.nonNegative(line.approvedAmount()), item.getClaimedAmount());
            BigDecimal lineRejected = Money.of(item.getClaimedAmount().subtract(lineApproved));
            item.setApprovedAmount(lineApproved);
            item.setRejectedAmount(lineRejected);
            if (lineRejected.signum() > 0) {
                String reason = line == null ? "No decision returned for this line" : line.rejectionReason();
                item.setRejectionReason(reason);
                rejectedByBillItem.put(item.getBillItem().getId(), lineRejected);
                firstReason = firstReason == null ? reason : firstReason;
            }
            approved = approved.add(lineApproved);
            rejected = rejected.add(lineRejected);
        }
        claim.applyDecision(approved, rejected, firstReason, now);

        if (!rejectedByBillItem.isEmpty()) {
            billingService.shiftRejectedBenefitToPatient(claim.getBill().getId(), PaidBy.from(claim.getPayerType()),
                    rejectedByBillItem);
        }

        if (approved.signum() == 0) {
            claim.transitionTo(ClaimStatus.REJECTED, decision.message(), now);
            eventPublisher.publish(ClaimEvent.of(ClaimEventType.CLAIM_REJECTED, event.tenantId(), claim.getId(),
                    claim.getClaimNo(), Money.of(rejected), decision.message()));
        } else {
            claim.transitionTo(ClaimStatus.ACCEPTED, "Approved $%s of $%s%s".formatted(Money.of(approved),
                    claim.getClaimedAmount(), rejected.signum() > 0 ? " (partially rejected: " + firstReason + ")" : ""), now);
            eventPublisher.publish(ClaimEvent.of(ClaimEventType.CLAIM_ADJUDICATED, event.tenantId(), claim.getId(),
                    claim.getClaimNo(), Money.of(approved), decision.message()));
        }
        log.info("Claim {} adjudicated: approved {}, rejected {}", claim.getClaimNo(), approved, rejected);
    }

    private void rejectForCommunicationFailure(ClaimEvent event, PayerCommunicationException ex) {
        InsuranceClaim claim = claimRepository.findWithItemsById(event.claimId()).orElseThrow();
        if (claim.getStatus() != ClaimStatus.SUBMISSION_QUEUED) {
            return;
        }
        String reason = "Payer unreachable after retries: " + ex.getMessage();
        claim.applyDecision(Money.ZERO, Money.ZERO, reason, LocalDateTime.now(clock));
        claim.transitionTo(ClaimStatus.REJECTED, reason, LocalDateTime.now(clock));
        eventPublisher.publish(ClaimEvent.of(ClaimEventType.CLAIM_REJECTED, event.tenantId(), claim.getId(),
                claim.getClaimNo(), claim.getClaimedAmount(), reason));
    }

    private ClaimSubmission toSubmission(InsuranceClaim claim) {
        Patient patient = claim.getAdmission().getPatient();
        InsurancePolicy policy = claim.getPolicy();
        boolean policyActive = policy == null || policy.isActiveOn(LocalDate.now(clock));
        return new ClaimSubmission(claim.getClaimNo(), claim.getPayerType(), claim.getCompany().getCompanyName(),
                policy == null ? null : policy.getPolicyNo(), policyActive, patient.fullName(),
                patient.getMedicareNo(), patient.getMedicareIrn(), claim.getAdmission().getDoctor().getProviderNo(),
                claim.getItems().stream()
                        .map(i -> new ClaimSubmission.Line(i.getId(), i.getBillItem().getItemType().name(),
                                i.getBillItem().getDescription(), i.getBillItem().getMbsItemNo(), i.getClaimedAmount()))
                        .toList());
    }
}
