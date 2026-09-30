package com.hospital.billing.claim.integration;

import com.hospital.billing.claim.integration.PayerDecision.LineDecision;
import com.hospital.billing.insurance.domain.PayerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sandbox payer for development and demos. Deterministic rules so every path can be shown:
 * <ul>
 *   <li>a private-fund claim on a policy that is no longer active is rejected in full;</li>
 *   <li>private funds reject EQUIPMENT lines ("not covered by hospital product");</li>
 *   <li>everything else is approved as claimed;</li>
 *   <li>{@code app.claims.mock-adapter.transient-failure-rate} (0.0-1.0) simulates outages
 *       so the exponential back-off retry can be observed in the logs.</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("!prod")
public class MockEclipseClaimAdapter implements InsuranceClaimAdapter {

    private final long latencyMs;
    private final double transientFailureRate;

    public MockEclipseClaimAdapter(@Value("${app.claims.mock-adapter.latency-ms:500}") long latencyMs,
                                   @Value("${app.claims.mock-adapter.transient-failure-rate:0.0}") double transientFailureRate) {
        this.latencyMs = latencyMs;
        this.transientFailureRate = transientFailureRate;
    }

    @Override
    public SubmissionReceipt submit(ClaimSubmission submission) {
        simulateNetwork();
        String prefix = submission.payerType() == PayerType.MEDICARE ? "ECL" : "FND";
        String reference = prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[MOCK {}] accepted claim {} for processing as {}", submission.payerName(), submission.claimNo(), reference);
        return new SubmissionReceipt(reference, Instant.now());
    }

    @Override
    public PayerDecision fetchDecision(String payerReference, ClaimSubmission submission) {
        simulateNetwork();
        if (submission.payerType() == PayerType.PRIVATE_FUND && !submission.policyActive()) {
            List<LineDecision> rejected = submission.lines().stream()
                    .map(l -> new LineDecision(l.claimItemId(), BigDecimal.ZERO, "Policy not active"))
                    .toList();
            return new PayerDecision(rejected, "Claim rejected: policy %s is not active".formatted(submission.policyNo()));
        }
        List<LineDecision> decisions = submission.lines().stream()
                .map(line -> {
                    if (submission.payerType() == PayerType.PRIVATE_FUND && "EQUIPMENT".equals(line.itemType())) {
                        return new LineDecision(line.claimItemId(), BigDecimal.ZERO,
                                "Item not covered by the member's hospital product");
                    }
                    return new LineDecision(line.claimItemId(), line.claimedAmount(), null);
                })
                .toList();
        return new PayerDecision(decisions, "Assessed by " + submission.payerName());
    }

    private void simulateNetwork() {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PayerCommunicationException("Interrupted while contacting payer", e);
        }
        if (transientFailureRate > 0 && ThreadLocalRandom.current().nextDouble() < transientFailureRate) {
            throw new PayerCommunicationException("Simulated payer gateway timeout");
        }
    }
}
