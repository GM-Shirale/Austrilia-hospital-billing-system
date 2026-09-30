package com.hospital.billing.consultation.web;

import com.hospital.billing.consultation.domain.Consultation;
import com.hospital.billing.consultation.domain.ConsultationStatus;
import com.hospital.billing.consultation.domain.ConsultationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ConsultationDtos {

    private ConsultationDtos() {
    }

    /**
     * @param mbsItemNo optional override of the doctor's default MBS item (e.g. 23 for an OPD GP visit)
     * @param fee       optional override of the doctor's consultation fee
     */
    public record ConsultationRequest(
            @NotNull(message = "Select a doctor") Long doctorId,
            @NotNull(message = "Select a consultation type") ConsultationType consultationType,
            LocalDateTime consultationAt,
            @Pattern(regexp = "^\\d{1,5}$", message = "MBS item must be 1-5 digits") String mbsItemNo,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal mbsScheduleFee,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal fee,
            @Size(max = 500) String notes) {
    }

    public record ConsultationResponse(Long id, String consultationNo, Long admissionId, Long doctorId,
                                       String doctorName, String providerNo, String specialization,
                                       ConsultationType consultationType, LocalDateTime consultationAt,
                                       String mbsItemNo, BigDecimal mbsScheduleFee, BigDecimal fee, String notes,
                                       ConsultationStatus status, boolean withinSchedule) {

        public static ConsultationResponse from(Consultation c, boolean withinSchedule) {
            return new ConsultationResponse(c.getId(), c.getConsultationNo(), c.getAdmission().getId(),
                    c.getDoctor().getId(), c.getDoctor().displayName(), c.getDoctor().getProviderNo(),
                    c.getDoctor().getSpecialization(), c.getConsultationType(), c.getConsultationAt(),
                    c.getMbsItemNo(), c.getMbsScheduleFee(), c.getFee(), c.getNotes(), c.getStatus(), withinSchedule);
        }
    }
}
