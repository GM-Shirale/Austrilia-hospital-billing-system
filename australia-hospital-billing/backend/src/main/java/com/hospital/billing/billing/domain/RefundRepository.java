package com.hospital.billing.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByPaymentIdOrderByRefundDateAsc(Long paymentId);
}
