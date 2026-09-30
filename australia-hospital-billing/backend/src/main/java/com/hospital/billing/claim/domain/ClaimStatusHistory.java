package com.hospital.billing.claim.domain;

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

/** Audit trail of every status change (shown as a timeline in the UI). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "claim_status_history")
public class ClaimStatusHistory extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private InsuranceClaim claim;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private ClaimStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private ClaimStatus toStatus;

    @Column(length = 500)
    private String note;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
