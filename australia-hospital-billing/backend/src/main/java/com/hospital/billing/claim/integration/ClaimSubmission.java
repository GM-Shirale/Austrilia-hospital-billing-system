package com.hospital.billing.claim.integration;

import com.hospital.billing.insurance.domain.PayerType;

import java.math.BigDecimal;
import java.util.List;

/** Payer-neutral claim payload handed to an {@link InsuranceClaimAdapter}. */
public record ClaimSubmission(String claimNo,
                              PayerType payerType,
                              String payerName,
                              String policyNo,
                              boolean policyActive,
                              String patientName,
                              String medicareNo,
                              Short medicareIrn,
                              String providerNo,
                              List<Line> lines) {

    public record Line(Long claimItemId, String itemType, String description, String mbsItemNo,
                       BigDecimal claimedAmount) {
    }
}
