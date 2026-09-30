package com.hospital.billing.claim.domain;

import com.hospital.billing.billing.domain.BillItem;
import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "claim_item")
public class ClaimItem extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private InsuranceClaim claim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_item_id", nullable = false)
    private BillItem billItem;

    @Column(name = "claimed_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal claimedAmount;

    @Column(name = "approved_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal approvedAmount = Money.ZERO;

    @Column(name = "rejected_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal rejectedAmount = Money.ZERO;

    @Column(name = "rejection_reason", length = 250)
    private String rejectionReason;
}
