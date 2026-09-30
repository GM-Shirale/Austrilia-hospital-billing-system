package com.hospital.billing.claim.event;

/** Kafka topic names (also used as logical channel names in in-memory mode). */
public final class ClaimTopics {

    /** Outbound claims queued for external adjudication. */
    public static final String CLAIM_SUBMITTED = "claim.submitted";
    /** Payer accepted the claim (fully or partially). */
    public static final String CLAIM_ADJUDICATED = "claim.adjudicated";
    /** Payer rejected the claim: denial routing (patient becomes liable). */
    public static final String CLAIM_REJECTED = "claim.rejected";
    /** Payer paid: triggers EOB generation and financial reconciliation. */
    public static final String REMITTANCE_POSTED = "remittance.posted";
    /** Async patient / staff email and SMS alerts. */
    public static final String NOTIFICATION_DISPATCH = "notification.dispatch";

    private ClaimTopics() {
    }
}
