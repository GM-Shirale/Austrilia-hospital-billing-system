package com.hospital.billing.pharmacy.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
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

/** Stock ledger row: every change of a medicine's stock, with who, when and why. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medicine_stock_movement")
public class StockMovement extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false, updatable = false)
    private Medicine medicine;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType movementType;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(length = 250)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    @Column(name = "performed_by_name", nullable = false, length = 120)
    private String performedByName;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
}
