package com.hospital.billing.billing.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.exception.BusinessRuleException;
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
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment")
public class Payment extends BaseTenantEntity {

    @Column(name = "payment_no", nullable = false, length = 30, updatable = false)
    private String paymentNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_bill_id", nullable = false)
    private HospitalBill bill;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = Money.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 30)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_by", nullable = false, length = 20)
    private PaidBy paidBy;

    @Column(name = "transaction_no", length = 60)
    private String transactionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.COMPLETED;

    public BigDecimal refundableAmount() {
        return Money.nonNegative(amount.subtract(refundedAmount));
    }

    public void refund(BigDecimal value) {
        if (value.compareTo(refundableAmount()) > 0) {
            throw new BusinessRuleException("Refund of %s exceeds the refundable amount of %s"
                    .formatted(value, refundableAmount()));
        }
        refundedAmount = Money.of(refundedAmount.add(value));
        status = refundedAmount.compareTo(amount) >= 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }
}
