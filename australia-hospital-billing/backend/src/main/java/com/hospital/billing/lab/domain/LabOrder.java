package com.hospital.billing.lab.domain;

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
@Table(name = "lab_order")
public class LabOrder extends BaseTenantEntity {

    @Column(name = "order_no", nullable = false, length = 30, updatable = false)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_id", nullable = false)
    private Admission admission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LabOrderStatus status = LabOrderStatus.ORDERED;

    @OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LabOrderItem> items = new ArrayList<>();

    // ------------------------------------------------------------ audit trail (who did what)

    @Column(name = "ordered_by_user_id")
    private Long orderedByUserId;

    @Column(name = "ordered_by_name", length = 120)
    private String orderedByName;

    @Column(name = "collected_by_user_id")
    private Long collectedByUserId;

    @Column(name = "collected_by_name", length = 120)
    private String collectedByName;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    /** Lab technician / scientist who completed the order. */
    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    @Column(name = "performed_by_name", length = 120)
    private String performedByName;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "result_notes", length = 1000)
    private String resultNotes;

    @Column(name = "cancelled_by_name", length = 120)
    private String cancelledByName;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** Moves the order along its lifecycle and stamps who did it and when. */
    public void transition(LabOrderStatus target, Long userId, String userName, LocalDateTime at, String notes) {
        transitionTo(target);
        switch (target) {
            case COLLECTED -> {
                collectedByUserId = userId;
                collectedByName = userName;
                collectedAt = at;
            }
            case COMPLETED -> {
                performedByUserId = userId;
                performedByName = userName;
                completedAt = at;
                if (notes != null && !notes.isBlank()) {
                    resultNotes = notes.trim();
                }
            }
            case CANCELLED -> {
                cancelledByName = userName;
                cancelledAt = at;
                if (notes != null && !notes.isBlank()) {
                    resultNotes = "Cancelled: " + notes.trim();
                }
            }
            default -> {
                // ORDERED is the initial state only
            }
        }
    }

    public void addItem(LabOrderItem item) {
        item.setLabOrder(this);
        items.add(item);
    }

    public void transitionTo(LabOrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Lab order", status, target);
        }
        status = target;
    }
}
