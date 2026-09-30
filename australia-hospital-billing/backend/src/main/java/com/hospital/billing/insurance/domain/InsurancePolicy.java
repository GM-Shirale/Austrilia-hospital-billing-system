package com.hospital.billing.insurance.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.patient.domain.Patient;
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
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "insurance_policy")
public class InsurancePolicy extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private InsuranceCompany company;

    @Column(name = "policy_no", nullable = false, length = 40)
    private String policyNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "cover_tier", nullable = false, length = 20)
    private CoverTier coverTier;

    /** Paid by the patient once per admission before the fund pays hospital charges. */
    @Column(name = "excess_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal excessAmount = Money.ZERO;

    @Column(name = "annual_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal annualLimit;

    /** Benefit accumulator: benefits already paid this policy year. */
    @Column(name = "benefits_used", nullable = false, precision = 12, scale = 2)
    private BigDecimal benefitsUsed = Money.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    public boolean isActiveOn(LocalDate date) {
        return status == PolicyStatus.ACTIVE
                && !date.isBefore(startDate)
                && (endDate == null || !date.isAfter(endDate));
    }

    public BigDecimal remainingAnnualLimit() {
        return Money.nonNegative(annualLimit.subtract(benefitsUsed));
    }

    public void recordBenefitPaid(BigDecimal amount) {
        this.benefitsUsed = Money.of(benefitsUsed.add(amount));
    }
}
