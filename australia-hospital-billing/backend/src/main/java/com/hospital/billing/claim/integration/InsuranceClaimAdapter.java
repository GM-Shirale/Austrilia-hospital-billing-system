package com.hospital.billing.claim.integration;

/**
 * Dependency Inversion + Liskov Substitution: the claims workflow depends on this contract
 * only. {@link MockEclipseClaimAdapter} (dev/test) and {@link ProductionEclipseClaimAdapter}
 * (profile "prod") are interchangeable; the active Spring profile decides which bean exists.
 *
 * <p>Contract every implementation must honour (LSP):</p>
 * <ul>
 *   <li>transient problems throw {@link PayerCommunicationException} (caller retries);</li>
 *   <li>business rejections are returned in {@link PayerDecision}, never thrown;</li>
 *   <li>{@code fetchDecision} returns one line decision per submitted line.</li>
 * </ul>
 */
public interface InsuranceClaimAdapter {

    SubmissionReceipt submit(ClaimSubmission submission);

    PayerDecision fetchDecision(String payerReference, ClaimSubmission submission);
}
