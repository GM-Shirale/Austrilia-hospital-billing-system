package com.hospital.billing.claim.web;

import com.hospital.billing.claim.domain.ClaimItem;
import com.hospital.billing.claim.domain.ClaimStatus;
import com.hospital.billing.claim.domain.ClaimStatusHistory;
import com.hospital.billing.claim.domain.ExplanationOfBenefit;
import com.hospital.billing.claim.domain.InsuranceClaim;
import com.hospital.billing.insurance.domain.PayerType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ClaimDtos {

    private ClaimDtos() {
    }

    public record ClaimSummary(Long id, String claimNo, ClaimStatus status, PayerType payerType, String payerName,
                               Long billId, String billNo, String admissionNo, String patientName,
                               LocalDateTime claimDate, BigDecimal claimedAmount, BigDecimal approvedAmount,
                               BigDecimal rejectedAmount, BigDecimal paidAmount) {

        public static ClaimSummary from(InsuranceClaim c) {
            return new ClaimSummary(c.getId(), c.getClaimNo(), c.getStatus(), c.getPayerType(),
                    c.getCompany().getCompanyName(), c.getBill().getId(), c.getBill().getBillNo(),
                    c.getAdmission().getAdmissionNo(), c.getAdmission().getPatient().fullName(), c.getClaimDate(),
                    c.getClaimedAmount(), c.getApprovedAmount(), c.getRejectedAmount(), c.getPaidAmount());
        }
    }

    public record ClaimItemResponse(Long id, Long billItemId, String itemType, String description, String mbsItemNo,
                                    BigDecimal claimedAmount, BigDecimal approvedAmount, BigDecimal rejectedAmount,
                                    String rejectionReason) {

        public static ClaimItemResponse from(ClaimItem i) {
            return new ClaimItemResponse(i.getId(), i.getBillItem().getId(), i.getBillItem().getItemType().name(),
                    i.getBillItem().getDescription(), i.getBillItem().getMbsItemNo(), i.getClaimedAmount(),
                    i.getApprovedAmount(), i.getRejectedAmount(), i.getRejectionReason());
        }
    }

    public record StatusHistoryResponse(ClaimStatus fromStatus, ClaimStatus toStatus, String note,
                                        LocalDateTime changedAt) {

        public static StatusHistoryResponse from(ClaimStatusHistory h) {
            return new StatusHistoryResponse(h.getFromStatus(), h.getToStatus(), h.getNote(), h.getChangedAt());
        }
    }

    public record EobResponse(Long id, String eobNo, LocalDateTime issuedAt, BigDecimal totalCharged,
                              BigDecimal totalBenefit, BigDecimal patientResponsibility, String summary) {

        public static EobResponse from(ExplanationOfBenefit e) {
            return new EobResponse(e.getId(), e.getEobNo(), e.getIssuedAt(), e.getTotalCharged(), e.getTotalBenefit(),
                    e.getPatientResponsibility(), e.getSummary());
        }
    }

    public record ClaimResponse(ClaimSummary claim, String policyNo, String payerReference, String rejectionReason,
                                LocalDateTime submittedAt, LocalDateTime adjudicatedAt, LocalDateTime paidAt,
                                List<ClaimStatus> allowedNextStatuses, List<ClaimItemResponse> items,
                                List<StatusHistoryResponse> history, EobResponse eob) {
    }
}
