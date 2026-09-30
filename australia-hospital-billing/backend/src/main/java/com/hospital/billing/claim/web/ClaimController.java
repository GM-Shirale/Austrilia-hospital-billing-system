package com.hospital.billing.claim.web;

import com.hospital.billing.claim.ai.ClaimRiskReviewService;
import com.hospital.billing.claim.ai.ClaimRiskReviewService.ClaimReviewResponse;
import com.hospital.billing.claim.domain.ClaimStatus;
import com.hospital.billing.claim.service.ClaimService;
import com.hospital.billing.claim.web.ClaimDtos.ClaimResponse;
import com.hospital.billing.claim.web.ClaimDtos.ClaimSummary;
import com.hospital.billing.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Insurance claims")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('BILLING_OFFICER')")
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimRiskReviewService claimRiskReviewService;

    public record CloseRequest(@Size(max = 500) String note) {
    }

    @GetMapping("/claims")
    public PageResponse<ClaimSummary> search(
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 20, sort = "claimDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return claimService.search(status, pageable);
    }

    @GetMapping("/claims/{id}")
    public ClaimResponse get(@PathVariable Long id) {
        return claimService.get(id);
    }

    @Operation(summary = "Pre-submission risk review (rules + Spring AI narrative)")
    @GetMapping("/claims/{id}/ai-review")
    public ClaimReviewResponse review(@PathVariable Long id) {
        return claimRiskReviewService.review(id);
    }

    @GetMapping("/bills/{billId}/claims")
    public List<ClaimSummary> forBill(@PathVariable Long billId) {
        return claimService.claimsForBill(billId);
    }

    @Operation(summary = "Create Medicare and/or private fund claims from a finalised bill")
    @PostMapping("/bills/{billId}/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ClaimSummary> create(@PathVariable Long billId) {
        return claimService.createClaimsForBill(billId);
    }

    @Operation(summary = "Run pre-submission checks (DRAFT -> VALIDATED)")
    @PostMapping("/claims/{id}/validate")
    public ClaimResponse validate(@PathVariable Long id) {
        return claimService.validate(id);
    }

    @Operation(summary = "Queue for lodgement (VALIDATED -> SUBMISSION_QUEUED); processing continues asynchronously")
    @PostMapping("/claims/{id}/submit")
    public ClaimResponse submit(@PathVariable Long id) {
        return claimService.submit(id);
    }

    @PostMapping("/claims/{id}/close")
    public ClaimResponse close(@PathVariable Long id, @RequestBody(required = false) CloseRequest request) {
        return claimService.close(id, request == null ? null : request.note());
    }
}
