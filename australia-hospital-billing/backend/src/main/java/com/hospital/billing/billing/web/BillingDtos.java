package com.hospital.billing.billing.web;

import com.hospital.billing.adjudication.CoverageProfile;
import com.hospital.billing.admission.domain.AdmissionStatus;
import com.hospital.billing.admission.domain.CareSetting;
import com.hospital.billing.billing.domain.BillPaymentStatus;
import com.hospital.billing.admission.domain.FinancialClass;
import com.hospital.billing.billing.domain.BillItem;
import com.hospital.billing.billing.domain.BillItemType;
import com.hospital.billing.billing.domain.BillStatus;
import com.hospital.billing.billing.domain.HospitalBill;
import com.hospital.billing.billing.domain.PaidBy;
import com.hospital.billing.billing.domain.Payment;
import com.hospital.billing.billing.domain.PaymentMode;
import com.hospital.billing.billing.domain.PaymentStatus;
import com.hospital.billing.billing.domain.Refund;
import com.hospital.billing.insurance.domain.CoverTier;
import com.hospital.billing.insurance.domain.NetworkStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BillingDtos {

    private BillingDtos() {
    }

    public record GenerateBillRequest(@NotNull Long admissionId) {
    }

    public record ManualItemRequest(
            @NotNull BillItemType itemType,
            @NotBlank @Size(max = 250) String description,
            @Size(max = 10) String mbsItemNo,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal mbsScheduleFee,
            @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal discountAmount) {
    }

    public record CoverageSummary(FinancialClass financialClass, boolean medicareEligible, String fundName,
                                  String policyNo, CoverTier coverTier, NetworkStatus networkStatus,
                                  BigDecimal excessAmount, BigDecimal remainingAnnualLimit) {

        public static CoverageSummary from(CoverageProfile profile) {
            CoverageProfile.PrivateCover cover = profile.privateCover();
            return cover == null
                    ? new CoverageSummary(profile.financialClass(), profile.medicareEligible(), null, null, null,
                    null, null, null)
                    : new CoverageSummary(profile.financialClass(), profile.medicareEligible(), cover.fundName(),
                    cover.policyNo(), cover.coverTier(), cover.networkStatus(), cover.excessAmount(),
                    cover.remainingAnnualLimit());
        }
    }

    public record BillItemResponse(Long id, BillItemType itemType, Long referenceId, String description,
                                   String mbsItemNo, BigDecimal mbsScheduleFee, BigDecimal quantity,
                                   BigDecimal unitPrice, BigDecimal grossAmount, BigDecimal discountAmount,
                                   BigDecimal gstAmount, BigDecimal netAmount, BigDecimal medicareBenefit,
                                   BigDecimal insuranceCoveredAmount, BigDecimal patientAmount, boolean manual,
                                   boolean locked) {

        public static BillItemResponse from(BillItem i) {
            return new BillItemResponse(i.getId(), i.getItemType(), i.getReferenceId(), i.getDescription(),
                    i.getMbsItemNo(), i.getMbsScheduleFee(), i.getQuantity(), i.getUnitPrice(), i.getGrossAmount(),
                    i.getDiscountAmount(), i.getGstAmount(), i.getNetAmount(), i.getMedicareBenefit(),
                    i.getInsuranceCoveredAmount(), i.getPatientAmount(), i.isManual(), i.isLocked());
        }
    }

    /** Per-category totals, e.g. Accommodation / Consultation / Pathology / Pharmacy. */
    public record CategorySubtotal(BillItemType itemType, int lines, BigDecimal netAmount,
                                   BigDecimal medicareBenefit, BigDecimal insuranceAmount, BigDecimal patientAmount) {

        public static List<CategorySubtotal> of(List<BillItem> items) {
            Map<BillItemType, CategorySubtotal> totals = new EnumMap<>(BillItemType.class);
            for (BillItem i : items) {
                totals.merge(i.getItemType(),
                        new CategorySubtotal(i.getItemType(), 1, i.getNetAmount(), i.getMedicareBenefit(),
                                i.getInsuranceCoveredAmount(), i.getPatientAmount()),
                        (a, b) -> new CategorySubtotal(a.itemType(), a.lines() + b.lines(),
                                a.netAmount().add(b.netAmount()), a.medicareBenefit().add(b.medicareBenefit()),
                                a.insuranceAmount().add(b.insuranceAmount()), a.patientAmount().add(b.patientAmount())));
            }
            return List.copyOf(totals.values());
        }
    }

    public record BillSummary(Long id, String billNo, BillStatus status, BillPaymentStatus paymentStatus,
                              LocalDateTime billDate, Long admissionId, String admissionNo, String patientName,
                              BigDecimal netAmount, BigDecimal patientPayableAmount, BigDecimal paidAmount,
                              BigDecimal outstandingAmount, BigDecimal patientBalance) {

        public static BillSummary from(HospitalBill b) {
            return new BillSummary(b.getId(), b.getBillNo(), b.getStatus(), b.getPaymentStatus(), b.getBillDate(),
                    b.getAdmission().getId(), b.getAdmission().getAdmissionNo(),
                    b.getAdmission().getPatient().fullName(), b.getNetAmount(), b.getPatientPayableAmount(),
                    b.getPaidAmount(), b.outstandingAmount(), b.patientBalance());
        }
    }

    public record BillResponse(Long id, String billNo, BillStatus status, BillPaymentStatus paymentStatus,
                               LocalDateTime billDate,
                               Long admissionId, String admissionNo, AdmissionStatus admissionStatus,
                               CareSetting careSetting, LocalDateTime admissionDate, LocalDateTime dischargeDate,
                               long lengthOfStayDays,
                               Long patientId, String patientName, String patientMrn, String patientMedicareNo,
                               Short patientMedicareIrn, String doctorName, String doctorProviderNo,
                               BigDecimal grossAmount, BigDecimal discountAmount, BigDecimal gstAmount,
                               BigDecimal netAmount, BigDecimal medicareAmount, BigDecimal insuranceAmount,
                               BigDecimal patientPayableAmount, BigDecimal paidAmount, BigDecimal outstandingAmount,
                               BigDecimal patientPaidAmount, BigDecimal patientBalance,
                               LocalDateTime chargesLockedAt,
                               CoverageSummary coverage, List<String> adjudicationNotes, List<String> warnings,
                               List<CategorySubtotal> subtotals,
                               List<BillItemResponse> items) {

        public static BillResponse from(HospitalBill b, CoverageSummary coverage, List<String> notes,
                                        List<String> warnings) {
            var admission = b.getAdmission();
            var patient = admission.getPatient();
            var doctor = admission.getDoctor();
            return new BillResponse(b.getId(), b.getBillNo(), b.getStatus(), b.getPaymentStatus(), b.getBillDate(),
                    admission.getId(), admission.getAdmissionNo(), admission.getStatus(), admission.getCareSetting(),
                    admission.getAdmissionDate(), admission.getDischargeDate(),
                    admission.lengthOfStayDays(LocalDateTime.now()),
                    patient.getId(), patient.fullName(), patient.getMrn(), patient.getMedicareNo(),
                    patient.getMedicareIrn(), doctor.displayName(), doctor.getProviderNo(),
                    b.getGrossAmount(), b.getDiscountAmount(), b.getGstAmount(), b.getNetAmount(),
                    b.getMedicareAmount(), b.getInsuranceAmount(), b.getPatientPayableAmount(), b.getPaidAmount(),
                    b.outstandingAmount(), b.getPatientPaidAmount(), b.patientBalance(), b.getChargesLockedAt(),
                    coverage, notes, warnings, CategorySubtotal.of(b.getItems()),
                    b.getItems().stream().map(BillItemResponse::from).toList());
        }
    }

    public record PaymentRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull PaymentMode paymentMode,
            @Size(max = 60) String transactionNo) {
    }

    public record RefundRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotBlank @Size(max = 250) String reason) {
    }

    public record RefundResponse(Long id, String refundNo, LocalDateTime refundDate, BigDecimal amount,
                                 String reason, String status) {

        public static RefundResponse from(Refund r) {
            return new RefundResponse(r.getId(), r.getRefundNo(), r.getRefundDate(), r.getAmount(), r.getReason(),
                    r.getStatus());
        }
    }

    public record PaymentResponse(Long id, String paymentNo, Long billId, LocalDateTime paymentDate,
                                  BigDecimal amount, BigDecimal refundedAmount, PaymentMode paymentMode,
                                  PaidBy paidBy, String transactionNo, PaymentStatus status,
                                  List<RefundResponse> refunds) {

        public static PaymentResponse from(Payment p, List<RefundResponse> refunds) {
            return new PaymentResponse(p.getId(), p.getPaymentNo(), p.getBill().getId(), p.getPaymentDate(),
                    p.getAmount(), p.getRefundedAmount(), p.getPaymentMode(), p.getPaidBy(), p.getTransactionNo(),
                    p.getStatus(), refunds);
        }
    }
}
