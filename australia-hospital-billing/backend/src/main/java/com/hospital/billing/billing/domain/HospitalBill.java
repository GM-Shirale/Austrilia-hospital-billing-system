package com.hospital.billing.billing.domain;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.InvalidStateTransitionException;
import com.hospital.billing.common.money.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Aggregate root for billing: totals are ALWAYS derived from the items
 * ({@link #recalculateTotals()}), never typed in, so they cannot drift apart.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "hospital_bill")
public class HospitalBill extends BaseTenantEntity {

    @Column(name = "bill_no", nullable = false, length = 30, updatable = false)
    private String billNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_id", nullable = false)
    private Admission admission;

    @Column(name = "bill_date", nullable = false)
    private LocalDateTime billDate;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount = Money.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = Money.ZERO;

    @Column(name = "gst_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal gstAmount = Money.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount = Money.ZERO;

    @Column(name = "medicare_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal medicareAmount = Money.ZERO;

    @Column(name = "insurance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal insuranceAmount = Money.ZERO;

    /** Patient payable = Net - Medicare - Insurance (ER diagram note 4). */
    @Column(name = "patient_payable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal patientPayableAmount = Money.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = Money.ZERO;

    /** Portion of paidAmount paid by the patient (Medicare / fund remittances excluded). */
    @Column(name = "patient_paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal patientPaidAmount = Money.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillStatus status = BillStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 10)
    private BillPaymentStatus paymentStatus = BillPaymentStatus.PENDING;

    /** Set at discharge: clinical charges are frozen and their lines cannot be removed. */
    @Column(name = "charges_locked_at")
    private LocalDateTime chargesLockedAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<BillItem> items = new ArrayList<>();

    public void addItem(BillItem item) {
        requireDraft();
        item.setBill(this);
        items.add(item);
    }

    public void removeItem(BillItem item) {
        requireDraft();
        if (item.isLocked()) {
            throw new BusinessRuleException("'%s' was locked at discharge and cannot be removed; add a credit line instead"
                    .formatted(item.getDescription()));
        }
        items.remove(item);
        item.setBill(null);
    }

    public void recalculateTotals() {
        grossAmount = sum(BillItem::getGrossAmount);
        discountAmount = sum(BillItem::getDiscountAmount);
        gstAmount = sum(BillItem::getGstAmount);
        netAmount = sum(BillItem::getNetAmount);
        medicareAmount = sum(BillItem::getMedicareBenefit);
        insuranceAmount = sum(BillItem::getInsuranceCoveredAmount);
        patientPayableAmount = sum(BillItem::getPatientAmount);
        refreshPaymentStatus();
    }

    /** Discharge: freeze every clinical (non-manual) line. */
    public void lockCharges(LocalDateTime at) {
        items.stream().filter(i -> !i.isManual()).forEach(i -> i.setLocked(true));
        chargesLockedAt = at;
    }

    public boolean isChargesLocked() {
        return chargesLockedAt != null;
    }

    /** Patient's remaining balance (patient payable - patient payments). */
    public BigDecimal patientBalance() {
        return Money.nonNegative(patientPayableAmount.subtract(patientPaidAmount));
    }

    public BigDecimal outstandingAmount() {
        return Money.nonNegative(netAmount.subtract(paidAmount));
    }

    public void finalizeBill() {
        requireDraft();
        if (items.isEmpty()) {
            throw new BusinessRuleException("Cannot finalise a bill without items");
        }
        status = BillStatus.FINALIZED;
        refreshPaymentStatus();
    }

    public void cancel() {
        if (status != BillStatus.DRAFT && status != BillStatus.FINALIZED) {
            throw new InvalidStateTransitionException("Bill", status, BillStatus.CANCELLED);
        }
        if (Money.isPositive(paidAmount)) {
            throw new BusinessRuleException("A bill with payments cannot be cancelled; refund the payments first");
        }
        status = BillStatus.CANCELLED;
    }

    public void registerPayment(BigDecimal amount, PaidBy payer) {
        requirePayable();
        if (amount.compareTo(outstandingAmount()) > 0) {
            throw new BusinessRuleException("Payment of %s exceeds the outstanding amount of %s"
                    .formatted(amount, outstandingAmount()));
        }
        paidAmount = Money.of(paidAmount.add(amount));
        if (payer == PaidBy.PATIENT) {
            patientPaidAmount = Money.of(patientPaidAmount.add(amount));
        }
        refreshPaymentStatus();
    }

    public void reversePayment(BigDecimal amount, PaidBy payer) {
        paidAmount = Money.nonNegative(paidAmount.subtract(amount));
        if (payer == PaidBy.PATIENT) {
            patientPaidAmount = Money.nonNegative(patientPaidAmount.subtract(amount));
        }
        refreshPaymentStatus();
    }

    public boolean isDraft() {
        return status == BillStatus.DRAFT;
    }

    public void requireDraft() {
        if (status != BillStatus.DRAFT) {
            throw new BusinessRuleException("Bill %s is %s; only DRAFT bills can be changed".formatted(billNo, status));
        }
    }

    public void requirePayable() {
        if (status != BillStatus.FINALIZED && status != BillStatus.PARTIALLY_PAID && status != BillStatus.PAID) {
            throw new BusinessRuleException("Bill %s must be finalised before payments or claims".formatted(billNo));
        }
    }

    private void refreshPaymentStatus() {
        if (status != BillStatus.CANCELLED && status != BillStatus.DRAFT) {
            if (paidAmount.signum() == 0) {
                status = BillStatus.FINALIZED;
            } else if (paidAmount.compareTo(netAmount) >= 0) {
                status = BillStatus.PAID;
            } else {
                status = BillStatus.PARTIALLY_PAID;
            }
        }
        paymentStatus = BillPaymentStatus.of(status, patientPayableAmount, patientPaidAmount);
    }

    private BigDecimal sum(Function<BillItem, BigDecimal> field) {
        return Money.of(items.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
