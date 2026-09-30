package com.hospital.billing.consultation.domain;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.provider.domain.Doctor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A doctor attendance during an admission (ward round, specialist review, OPD visit).
 * Billed as a DOCTOR line with its own MBS item; the fee and schedule fee are copied from
 * the doctor at booking time so later fee changes do not alter historic bills.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consultation")
public class Consultation extends BaseTenantEntity {

    @Column(name = "consultation_no", nullable = false, length = 30, updatable = false)
    private String consultationNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_id", nullable = false, updatable = false)
    private Admission admission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type", nullable = false, length = 20)
    private ConsultationType consultationType;

    @Column(name = "consultation_at", nullable = false)
    private LocalDateTime consultationAt;

    @Column(name = "mbs_item_no", nullable = false, length = 10)
    private String mbsItemNo;

    @Column(name = "mbs_schedule_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal mbsScheduleFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fee;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultationStatus status = ConsultationStatus.COMPLETED;

    public void cancel() {
        if (status == ConsultationStatus.CANCELLED) {
            throw new BusinessRuleException("Consultation %s is already cancelled".formatted(consultationNo));
        }
        admission.requireActive();      // lines are locked once the patient is discharged
        status = ConsultationStatus.CANCELLED;
    }
}
