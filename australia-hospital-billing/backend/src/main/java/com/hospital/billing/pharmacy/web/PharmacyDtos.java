package com.hospital.billing.pharmacy.web;

import com.hospital.billing.pharmacy.domain.Medicine;
import com.hospital.billing.pharmacy.domain.StockMovement;
import com.hospital.billing.pharmacy.domain.StockMovementType;
import com.hospital.billing.pharmacy.domain.SupplyCategory;
import com.hospital.billing.pharmacy.domain.Prescription;
import com.hospital.billing.pharmacy.domain.PrescriptionItem;
import com.hospital.billing.pharmacy.domain.PrescriptionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PharmacyDtos {

    private PharmacyDtos() {
    }

    /** GST is not entered: it follows the supply category (PBS / prescription GST-free, OTC / taxable 10%). */
    public record MedicineRequest(
            @NotBlank(message = "Name is required") @Size(max = 120) String medicineName,
            @NotBlank(message = "Category is required") @Size(max = 60) String category,
            @NotNull(message = "Unit price is required") @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
            @NotNull(message = "Select the GST / supply category") SupplyCategory supplyCategory,
            @Pattern(regexp = "^[0-9A-Z]{1,10}$", message = "PBS item code: up to 10 letters/digits") String pbsItemCode,
            @Min(0) Integer stockQuantity,
            Boolean active) {
    }

    public record StockAdjustmentRequest(
            @NotNull @Min(-100000) @Max(100000) Integer delta,
            @Size(max = 250) String reason) {
    }

    public record MedicineResponse(Long id, String medicineName, String category, BigDecimal unitPrice,
                                   SupplyCategory supplyCategory, BigDecimal gstRate, boolean gstFree,
                                   String pbsItemCode, Integer stockQuantity, boolean active,
                                   String stockAdjustedByName, LocalDateTime stockAdjustedAt) {

        public static MedicineResponse from(Medicine m) {
            return new MedicineResponse(m.getId(), m.getMedicineName(), m.getCategory(), m.getUnitPrice(),
                    m.getSupplyCategory(), m.getGstRate(), m.getSupplyCategory().gstFree(), m.getPbsItemCode(),
                    m.getStockQuantity(), m.isActive(), m.getStockAdjustedByName(), m.getStockAdjustedAt());
        }
    }

    public record StockMovementResponse(Long id, StockMovementType movementType, int quantityDelta, int balanceAfter,
                                        String reason, String prescriptionNo, String performedByName,
                                        LocalDateTime performedAt) {

        public static StockMovementResponse from(StockMovement m) {
            return new StockMovementResponse(m.getId(), m.getMovementType(), m.getQuantityDelta(), m.getBalanceAfter(),
                    m.getReason(), m.getPrescription() == null ? null : m.getPrescription().getPrescriptionNo(),
                    m.getPerformedByName(), m.getPerformedAt());
        }
    }

    public record PrescriptionRequest(
            @NotNull Long admissionId,
            @NotNull Long doctorId,
            @NotEmpty @Valid List<PrescriptionLine> items) {
    }

    public record PrescriptionLine(
            @NotNull Long medicineId,
            @NotNull @Min(1) @Max(1000) Integer quantity,
            @NotBlank @Size(max = 60) String dose,
            @NotBlank @Size(max = 60) String frequency) {
    }

    public record PrescriptionItemResponse(Long id, Long medicineId, String medicineName, Integer quantity,
                                           String dose, String frequency, BigDecimal unitPrice,
                                           SupplyCategory supplyCategory, BigDecimal gstRate, Integer stockAvailable) {

        public static PrescriptionItemResponse from(PrescriptionItem i) {
            Medicine m = i.getMedicine();
            return new PrescriptionItemResponse(i.getId(), m.getId(), m.getMedicineName(), i.getQuantity(), i.getDose(),
                    i.getFrequency(), i.getUnitPrice(), m.getSupplyCategory(), m.getGstRate(), m.getStockQuantity());
        }
    }

    /** Who did what on the prescription (UI badges). */
    public record PrescriptionAudit(String prescribedByName, String dispensedByName, LocalDateTime dispensedAt,
                                    String cancelledByName, LocalDateTime cancelledAt) {

        static PrescriptionAudit of(Prescription p) {
            return new PrescriptionAudit(p.getPrescribedByName(), p.getDispensedByName(), p.getDispensedAt(),
                    p.getCancelledByName(), p.getCancelledAt());
        }
    }

    public record PrescriptionResponse(Long id, String prescriptionNo, Long admissionId, String admissionNo,
                                       String patientName, String patientMrn, Long doctorId, String doctorName,
                                       LocalDateTime prescriptionDate, PrescriptionStatus status,
                                       boolean admissionActive, boolean stockSufficient, PrescriptionAudit audit,
                                       List<PrescriptionItemResponse> items) {

        public static PrescriptionResponse summary(Prescription p) {
            return of(p, List.of(), true);
        }

        public static PrescriptionResponse detail(Prescription p) {
            boolean enough = p.getItems().stream()
                    .allMatch(i -> i.getMedicine().getStockQuantity() >= i.getQuantity());
            return of(p, p.getItems().stream().map(PrescriptionItemResponse::from).toList(), enough);
        }

        private static PrescriptionResponse of(Prescription p, List<PrescriptionItemResponse> items, boolean enough) {
            return new PrescriptionResponse(p.getId(), p.getPrescriptionNo(), p.getAdmission().getId(),
                    p.getAdmission().getAdmissionNo(), p.getAdmission().getPatient().fullName(),
                    p.getAdmission().getPatient().getMrn(), p.getDoctor().getId(), p.getDoctor().displayName(),
                    p.getPrescriptionDate(), p.getStatus(), p.getAdmission().isActive(), enough,
                    PrescriptionAudit.of(p), items);
        }
    }
}
