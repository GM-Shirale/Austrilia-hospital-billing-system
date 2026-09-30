package com.hospital.billing.claim.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "explanation_of_benefit")
public class ExplanationOfBenefit extends BaseTenantEntity {

    @Column(name = "eob_no", nullable = false, length = 30, updatable = false)
    private String eobNo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private InsuranceClaim claim;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "total_charged", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCharged;

    @Column(name = "total_benefit", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBenefit;

    @Column(name = "patient_responsibility", nullable = false, precision = 12, scale = 2)
    private BigDecimal patientResponsibility;

    @Column(length = 1000)
    private String summary;
}
