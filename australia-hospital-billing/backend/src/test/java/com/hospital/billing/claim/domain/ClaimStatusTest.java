package com.hospital.billing.claim.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimStatusTest {

    @Test
    void happyPathIsAllowed() {
        ClaimStatus[] path = {ClaimStatus.DRAFT, ClaimStatus.VALIDATED, ClaimStatus.SUBMISSION_QUEUED,
                ClaimStatus.SUBMITTED, ClaimStatus.ACKNOWLEDGED, ClaimStatus.ACCEPTED, ClaimStatus.PAID,
                ClaimStatus.RECONCILED, ClaimStatus.CLOSED};
        for (int i = 0; i < path.length - 1; i++) {
            assertTrue(path[i].canTransitionTo(path[i + 1]), path[i] + " -> " + path[i + 1]);
        }
    }

    @Test
    void cannotSkipValidationOrPayARejectedClaim() {
        assertFalse(ClaimStatus.DRAFT.canTransitionTo(ClaimStatus.SUBMITTED));
        assertFalse(ClaimStatus.REJECTED.canTransitionTo(ClaimStatus.PAID));
        assertFalse(ClaimStatus.CLOSED.canTransitionTo(ClaimStatus.DRAFT));
    }

    @Test
    void payerCanRejectAtAnyStageBeforeAcceptance() {
        assertTrue(ClaimStatus.SUBMISSION_QUEUED.canTransitionTo(ClaimStatus.REJECTED));
        assertTrue(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.REJECTED));
        assertTrue(ClaimStatus.ACKNOWLEDGED.canTransitionTo(ClaimStatus.REJECTED));
        assertFalse(ClaimStatus.PAID.canTransitionTo(ClaimStatus.REJECTED));
    }
}
