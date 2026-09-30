package com.hospital.billing.claim.service;

import com.hospital.billing.claim.domain.ClaimStatus;
import com.hospital.billing.claim.web.ClaimDtos.ClaimResponse;
import com.hospital.billing.claim.web.ClaimDtos.ClaimSummary;
import com.hospital.billing.common.web.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClaimService {

    /** Creates one DRAFT claim per payer that has a benefit on the bill (Medicare and/or fund). */
    List<ClaimSummary> createClaimsForBill(Long billId);

    ClaimResponse validate(Long claimId);

    /** VALIDATED -> SUBMISSION_QUEUED and publishes claim.submitted; the rest is asynchronous. */
    ClaimResponse submit(Long claimId);

    ClaimResponse close(Long claimId, String note);

    ClaimResponse get(Long claimId);

    PageResponse<ClaimSummary> search(ClaimStatus status, Pageable pageable);

    List<ClaimSummary> claimsForBill(Long billId);
}
