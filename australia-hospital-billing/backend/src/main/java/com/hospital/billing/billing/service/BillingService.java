package com.hospital.billing.billing.service;

import java.time.LocalDateTime;
import com.hospital.billing.billing.domain.BillStatus;
import com.hospital.billing.billing.domain.HospitalBill;
import com.hospital.billing.billing.domain.PaidBy;
import com.hospital.billing.billing.web.BillingDtos.BillResponse;
import com.hospital.billing.billing.web.BillingDtos.BillSummary;
import com.hospital.billing.billing.web.BillingDtos.ManualItemRequest;
import com.hospital.billing.common.web.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;

public interface BillingService {

    /** Collects every charge of the admission into a DRAFT bill and estimates the payer split. */
    BillResponse generateBill(Long admissionId);

    /** Re-collects automatic charges (e.g. more bed days, new lab orders); manual lines are kept. */
    BillResponse recalculate(Long billId);

    BillResponse addManualItem(Long billId, ManualItemRequest request);

    BillResponse removeItem(Long billId, Long itemId);

    BillResponse finalizeBill(Long billId);

    /**
     * Discharge hook: creates the bill if needed, costs the full stay up to the discharge
     * time, runs adjudication and LOCKS the clinical lines.
     */
    BillResponse prepareDischargeBill(Long admissionId, LocalDateTime dischargedAt);

    /**
     * Keeps the admission's DRAFT bill in step with the clinical record (called when a lab
     * order, dispensed medicine, consultation or bed changes). No-op when there is no bill yet
     * or the bill is locked / finalised.
     */
    void syncDraftBill(Long admissionId);

    BillResponse cancel(Long billId);

    BillResponse get(Long billId);

    PageResponse<BillSummary> search(BillStatus status, Long admissionId, Pageable pageable);

    /** For the payment and claim modules. */
    HospitalBill requireBill(Long billId);

    /** A payer rejected part of its benefit: move it to the patient's gap, line by line. */
    void shiftRejectedBenefitToPatient(Long billId, PaidBy payer, Map<Long, BigDecimal> rejectedByBillItem);
}
