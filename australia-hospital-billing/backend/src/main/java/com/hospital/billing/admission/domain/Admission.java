package com.hospital.billing.admission.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.InvalidStateTransitionException;
import com.hospital.billing.patient.domain.Patient;
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

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "admission")
public class Admission extends BaseTenantEntity {

    @Column(name = "admission_no", nullable = false, length = 30, updatable = false)
    private String admissionNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_type_id", nullable = false)
    private AdmissionType admissionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "financial_class", nullable = false, length = 20)
    private FinancialClass financialClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "care_setting", nullable = false, length = 10)
    private CareSetting careSetting = CareSetting.IPD;

    @Column(name = "admission_date", nullable = false)
    private LocalDateTime admissionDate;

    @Column(name = "discharge_date")
    private LocalDateTime dischargeDate;

    @Column(length = 500)
    private String diagnosis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdmissionStatus status = AdmissionStatus.ADMITTED;

    public boolean isActive() {
        return status == AdmissionStatus.ADMITTED;
    }

    public void discharge(LocalDateTime at) {
        transitionTo(AdmissionStatus.DISCHARGED);
        if (at.isBefore(admissionDate)) {
            throw new BusinessRuleException("Discharge date cannot be before the admission date");
        }
        this.dischargeDate = at;
    }

    /**
     * Length of stay in days, counted like accommodation: every started 24 hours is a day,
     * minimum 1 (a same-day stay is one day).
     */
    public long lengthOfStayDays(LocalDateTime asOf) {
        LocalDateTime end = dischargeDate != null ? dischargeDate : asOf;
        long minutes = Math.max(0, java.time.Duration.between(admissionDate, end).toMinutes());
        return Math.max(1, (minutes + (24 * 60) - 1) / (24 * 60));
    }

    public void changeAttendingDoctor(Doctor newDoctor) {
        requireActive();
        this.doctor = newDoctor;
    }

    public void cancel() {
        transitionTo(AdmissionStatus.CANCELLED);
    }

    public void requireActive() {
        if (!isActive()) {
            throw new BusinessRuleException("Admission %s is %s; the action requires an active admission"
                    .formatted(admissionNo, status));
        }
    }

    private void transitionTo(AdmissionStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Admission", status, target);
        }
        this.status = target;
    }
}
