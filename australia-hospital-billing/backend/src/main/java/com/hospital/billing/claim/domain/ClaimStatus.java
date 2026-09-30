package com.hospital.billing.claim.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Claim lifecycle state machine:
 * <pre>
 * DRAFT -> VALIDATED -> SUBMISSION_QUEUED -> SUBMITTED -> ACKNOWLEDGED -> ACCEPTED -> PAID -> RECONCILED -> CLOSED
 *                              |                |              |
 *                              +----------------+--------------+--> REJECTED -> CLOSED
 * </pre>
 * Allowed transitions are data, not scattered if-statements.
 */
public enum ClaimStatus {
    DRAFT,
    VALIDATED,
    SUBMISSION_QUEUED,
    SUBMITTED,
    ACKNOWLEDGED,
    ACCEPTED,
    REJECTED,
    PAID,
    RECONCILED,
    CLOSED;

    private static final Map<ClaimStatus, Set<ClaimStatus>> TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(VALIDATED, CLOSED),
            VALIDATED, EnumSet.of(SUBMISSION_QUEUED, DRAFT, CLOSED),
            SUBMISSION_QUEUED, EnumSet.of(SUBMITTED, REJECTED),
            SUBMITTED, EnumSet.of(ACKNOWLEDGED, REJECTED),
            ACKNOWLEDGED, EnumSet.of(ACCEPTED, REJECTED),
            ACCEPTED, EnumSet.of(PAID),
            REJECTED, EnumSet.of(CLOSED),
            PAID, EnumSet.of(RECONCILED),
            RECONCILED, EnumSet.of(CLOSED),
            CLOSED, EnumSet.noneOf(ClaimStatus.class));

    public boolean canTransitionTo(ClaimStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
