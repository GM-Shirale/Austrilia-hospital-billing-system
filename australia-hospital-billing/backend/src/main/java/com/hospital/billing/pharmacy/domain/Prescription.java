package com.hospital.billing.pharmacy.domain;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.exception.InvalidStateTransitionException;
import com.hospital.billing.provider.domain.Doctor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pharmacy_prescription")
public class Prescription extends BaseTenantEntity {

    @Column(name = "prescription_no", nullable = false, length = 30, updatable = false)
    private String prescriptionNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_id", nullable = false)
    private Admission admission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "prescription_date", nullable = false)
    private LocalDateTime prescriptionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrescriptionStatus status = PrescriptionStatus.PRESCRIBED;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> items = new ArrayList<>();

    // ------------------------------------------------------------ audit trail (who did what)

    @Column(name = "prescribed_by_user_id")
    private Long prescribedByUserId;

    @Column(name = "prescribed_by_name", length = 120)
    private String prescribedByName;

    /** Pharmacist who dispensed. */
    @Column(name = "dispensed_by_user_id")
    private Long dispensedByUserId;

    @Column(name = "dispensed_by_name", length = 120)
    private String dispensedByName;

    @Column(name = "dispensed_at")
    private LocalDateTime dispensedAt;

    @Column(name = "cancelled_by_name", length = 120)
    private String cancelledByName;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public void addItem(PrescriptionItem item) {
        item.setPrescription(this);
        items.add(item);
    }

    /**
     * Marks the prescription dispensed. Stock is deducted by the service (one ledger row per
     * line); the whole prescription succeeds or fails in one transaction.
     */
    public void markDispensed(Long userId, String userName, LocalDateTime at) {
        if (status != PrescriptionStatus.PRESCRIBED) {
            throw new InvalidStateTransitionException("Prescription", status, PrescriptionStatus.DISPENSED);
        }
        status = PrescriptionStatus.DISPENSED;
        dispensedByUserId = userId;
        dispensedByName = userName;
        dispensedAt = at;
    }

    public void cancel(String userName, LocalDateTime at) {
        if (status != PrescriptionStatus.PRESCRIBED) {
            throw new InvalidStateTransitionException("Prescription", status, PrescriptionStatus.CANCELLED);
        }
        status = PrescriptionStatus.CANCELLED;
        cancelledByName = userName;
        cancelledAt = at;
    }
}
