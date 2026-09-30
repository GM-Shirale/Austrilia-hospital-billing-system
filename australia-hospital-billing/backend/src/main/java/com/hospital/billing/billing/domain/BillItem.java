package com.hospital.billing.billing.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.money.Money;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bill_item")
public class BillItem extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_bill_id", nullable = false)
    private HospitalBill bill;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private BillItemType itemType;

    /** Id of the source record (allocation, doctor, lab item, prescription item); null = manual line. */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(nullable = false, length = 250)
    private String description;

    @Column(name = "mbs_item_no", length = 10)
    private String mbsItemNo;

    @Column(name = "mbs_schedule_fee", precision = 12, scale = 2)
    private BigDecimal mbsScheduleFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount = Money.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = Money.ZERO;

    @Column(name = "gst_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal gstAmount = Money.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount = Money.ZERO;

    @Column(name = "medicare_benefit", nullable = false, precision = 12, scale = 2)
    private BigDecimal medicareBenefit = Money.ZERO;

    @Column(name = "insurance_covered_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal insuranceCoveredAmount = Money.ZERO;

    @Column(name = "patient_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal patientAmount = Money.ZERO;

    /** Locked at discharge: the clinical charge can no longer be removed or re-collected. */
    @Column(nullable = false)
    private boolean locked;

    public boolean isManual() {
        return referenceId == null;
    }

    /** Benefit split produced by the AdjudicationEngine. */
    public void applySplit(BigDecimal medicare, BigDecimal insurance, BigDecimal patient) {
        this.medicareBenefit = Money.of(medicare);
        this.insuranceCoveredAmount = Money.of(insurance);
        this.patientAmount = Money.of(patient);
    }

    /** A payer rejected (part of) its benefit: the patient becomes liable for it. */
    public void shiftToPatient(PaidBy payer, BigDecimal amount) {
        BigDecimal value = Money.of(amount);
        if (payer == PaidBy.MEDICARE) {
            medicareBenefit = Money.nonNegative(medicareBenefit.subtract(value));
        } else if (payer == PaidBy.PRIVATE_FUND) {
            insuranceCoveredAmount = Money.nonNegative(insuranceCoveredAmount.subtract(value));
        }
        patientAmount = Money.of(patientAmount.add(value));
    }
}
