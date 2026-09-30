package com.hospital.billing.lab.web;

import com.hospital.billing.lab.domain.LabOrder;
import com.hospital.billing.lab.domain.LabOrderItem;
import com.hospital.billing.lab.domain.LabOrderStatus;
import com.hospital.billing.lab.domain.LabTest;
import com.hospital.billing.lab.domain.LabTestType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class LabDtos {

    private LabDtos() {
    }

    /**
     * Catalogue entry. An MBS item and schedule fee are needed for a Medicare rebate; tests
     * with an MBS item must be GST-free (pathology / imaging are GST-free medical services).
     */
    public record LabTestRequest(
            @NotBlank(message = "Code is required")
            @Pattern(regexp = "^[A-Za-z0-9_-]{2,20}$", message = "Code: 2-20 letters, digits, - or _") String testCode,
            @NotBlank(message = "Test name is required") @Size(max = 120) String testName,
            @NotBlank(message = "Category is required") @Size(max = 60) String category,
            @NotNull(message = "Select pathology, imaging or diagnostic") LabTestType testType,
            @NotNull(message = "Base fee is required") @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
            @Pattern(regexp = "^\\d{1,5}$", message = "MBS item must be 1-5 digits") String mbsItemNo,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal mbsScheduleFee,
            Boolean gstFree,
            Boolean active) {
    }

    public record LabTestResponse(Long id, String testCode, String testName, String category, LabTestType testType,
                                  BigDecimal unitPrice, String mbsItemNo, BigDecimal mbsScheduleFee, boolean gstFree,
                                  BigDecimal gstRate, boolean active) {

        public static LabTestResponse from(LabTest t) {
            return new LabTestResponse(t.getId(), t.getTestCode(), t.getTestName(), t.getCategory(), t.getTestType(),
                    t.getUnitPrice(), t.getMbsItemNo(), t.getMbsScheduleFee(), t.isGstFree(), t.gstRate(), t.isActive());
        }
    }

    public record LabOrderRequest(
            @NotNull Long admissionId,
            @NotNull Long doctorId,
            @NotEmpty @Valid List<LabOrderLine> items) {
    }

    public record LabOrderLine(@NotNull Long labTestId, @NotNull @Min(1) @Max(20) Integer quantity) {
    }

    /** @param notes result notes when completing, reason when cancelling */
    public record StatusUpdateRequest(@NotNull LabOrderStatus status, @Size(max = 1000) String notes) {
    }

    public record ResultRequest(@NotBlank @Size(max = 500) String result) {
    }

    public record LabOrderItemResponse(Long id, Long labTestId, String testCode, String testName, String mbsItemNo,
                                       LabTestType testType, Integer quantity, BigDecimal price, String result,
                                       String resultedByName, LocalDateTime resultedAt) {

        public static LabOrderItemResponse from(LabOrderItem i) {
            LabTest t = i.getLabTest();
            return new LabOrderItemResponse(i.getId(), t.getId(), t.getTestCode(), t.getTestName(), t.getMbsItemNo(),
                    t.getTestType(), i.getQuantity(), i.getPrice(), i.getResult(), i.getResultedByName(),
                    i.getResultedAt());
        }
    }

    /** Who did what on the order (UI badges). */
    public record LabAudit(String orderedByName, String collectedByName, LocalDateTime collectedAt,
                           String performedByName, LocalDateTime completedAt, String resultNotes,
                           String cancelledByName, LocalDateTime cancelledAt) {

        static LabAudit of(LabOrder o) {
            return new LabAudit(o.getOrderedByName(), o.getCollectedByName(), o.getCollectedAt(),
                    o.getPerformedByName(), o.getCompletedAt(), o.getResultNotes(), o.getCancelledByName(),
                    o.getCancelledAt());
        }
    }

    public record LabOrderResponse(Long id, String orderNo, Long admissionId, String admissionNo, String patientName,
                                   String patientMrn, Long doctorId, String doctorName, LocalDateTime orderDate,
                                   LabOrderStatus status, boolean admissionActive, LabAudit audit,
                                   List<LabOrderItemResponse> items) {

        public static LabOrderResponse summary(LabOrder o) {
            return of(o, List.of());
        }

        public static LabOrderResponse detail(LabOrder o) {
            return of(o, o.getItems().stream().map(LabOrderItemResponse::from).toList());
        }

        private static LabOrderResponse of(LabOrder o, List<LabOrderItemResponse> items) {
            return new LabOrderResponse(o.getId(), o.getOrderNo(), o.getAdmission().getId(),
                    o.getAdmission().getAdmissionNo(), o.getAdmission().getPatient().fullName(),
                    o.getAdmission().getPatient().getMrn(), o.getDoctor().getId(), o.getDoctor().displayName(),
                    o.getOrderDate(), o.getStatus(), o.getAdmission().isActive(), LabAudit.of(o), items);
        }
    }
}
