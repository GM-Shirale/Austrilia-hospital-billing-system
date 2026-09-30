package com.hospital.billing.claim.integration;

import java.math.BigDecimal;
import java.util.List;

/** Payer's adjudication response (an ECLIPSE "processing report" / fund assessment). */
public record PayerDecision(List<LineDecision> lines, String message) {

    public record LineDecision(Long claimItemId, BigDecimal approvedAmount, String rejectionReason) {
    }
}
