package com.hospital.billing.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBillIdOrderByPaymentDateAsc(Long billId);

    /** Net amount (payments minus refunds) from one payer for one bill; null if none (wrap with Money.of). */
    @Query("""
            select sum(p.amount - p.refundedAmount) from Payment p
            where p.bill.id = :billId and p.paidBy = :paidBy
            """)
    BigDecimal sumNetByBillAndPayer(@Param("billId") Long billId, @Param("paidBy") PaidBy paidBy);

    @Query("select sum(p.amount - p.refundedAmount) from Payment p")
    BigDecimal sumCollected();
}
