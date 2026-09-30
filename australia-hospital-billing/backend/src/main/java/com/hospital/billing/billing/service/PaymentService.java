package com.hospital.billing.billing.service;

import com.hospital.billing.billing.domain.HospitalBill;
import com.hospital.billing.billing.domain.PaidBy;
import com.hospital.billing.billing.domain.Payment;
import com.hospital.billing.billing.domain.PaymentMode;
import com.hospital.billing.billing.domain.PaymentRepository;
import com.hospital.billing.billing.domain.Refund;
import com.hospital.billing.billing.domain.RefundRepository;
import com.hospital.billing.billing.web.BillingDtos.PaymentRequest;
import com.hospital.billing.billing.web.BillingDtos.PaymentResponse;
import com.hospital.billing.billing.web.BillingDtos.RefundRequest;
import com.hospital.billing.billing.web.BillingDtos.RefundResponse;
import com.hospital.billing.common.domain.DocumentNumberGenerator;
import com.hospital.billing.common.domain.DocumentType;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.money.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Payments and refunds. Each payer can only pay its own share:
 * patient <= patient payable, Medicare <= Medicare benefit, fund <= fund benefit.
 * The bill's @Version column turns concurrent double-payments into a 409 instead of a lost update.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final Set<PaymentMode> REMITTANCE_MODES =
            EnumSet.of(PaymentMode.MEDICARE_REMITTANCE, PaymentMode.FUND_REMITTANCE);

    private final BillingService billingService;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final Clock clock;

    public List<PaymentResponse> paymentsForBill(Long billId) {
        billingService.requireBill(billId);
        return paymentRepository.findByBillIdOrderByPaymentDateAsc(billId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Front-desk payment by the patient. */
    @Transactional
    public PaymentResponse recordPatientPayment(Long billId, PaymentRequest request) {
        if (REMITTANCE_MODES.contains(request.paymentMode())) {
            throw new BusinessRuleException("Remittances are posted automatically by the claims workflow");
        }
        HospitalBill bill = billingService.requireBill(billId);
        BigDecimal amount = Money.of(request.amount());
        BigDecimal patientPaid = Money.of(paymentRepository.sumNetByBillAndPayer(billId, PaidBy.PATIENT));
        BigDecimal patientOwes = Money.nonNegative(bill.getPatientPayableAmount().subtract(patientPaid));
        if (amount.compareTo(patientOwes) > 0) {
            throw new BusinessRuleException("The patient owes %s; a payment of %s is too much".formatted(patientOwes, amount));
        }
        return toResponse(post(bill, amount, request.paymentMode(), PaidBy.PATIENT, request.transactionNo()));
    }

    /** Called by the claims workflow when Medicare or a fund pays. */
    @Transactional
    public Payment recordRemittance(Long billId, PaidBy payer, BigDecimal amount, String payerReference) {
        HospitalBill bill = billingService.requireBill(billId);
        BigDecimal share = payer == PaidBy.MEDICARE ? bill.getMedicareAmount() : bill.getInsuranceAmount();
        BigDecimal alreadyPaid = Money.of(paymentRepository.sumNetByBillAndPayer(billId, payer));
        BigDecimal payable = Money.min(Money.of(amount), Money.nonNegative(share.subtract(alreadyPaid)));
        if (payable.signum() <= 0) {
            throw new BusinessRuleException("Nothing is owed by %s on bill %s".formatted(payer, bill.getBillNo()));
        }
        PaymentMode mode = payer == PaidBy.MEDICARE ? PaymentMode.MEDICARE_REMITTANCE : PaymentMode.FUND_REMITTANCE;
        return post(bill, payable, mode, payer, payerReference);
    }

    @Transactional
    public PaymentResponse refund(Long paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment", paymentId));
        BigDecimal amount = Money.of(request.amount());
        payment.refund(amount);
        payment.getBill().reversePayment(amount, payment.getPaidBy());

        Refund refund = new Refund();
        refund.setRefundNo(documentNumberGenerator.next(DocumentType.REFUND));
        refund.setPayment(payment);
        refund.setRefundDate(LocalDateTime.now(clock));
        refund.setAmount(amount);
        refund.setReason(request.reason().trim());
        refundRepository.save(refund);
        log.info("Refunded {} on payment {}", amount, payment.getPaymentNo());
        return toResponse(payment);
    }

    private Payment post(HospitalBill bill, BigDecimal amount, PaymentMode mode, PaidBy payer, String reference) {
        bill.registerPayment(amount, payer);
        Payment payment = new Payment();
        payment.setPaymentNo(documentNumberGenerator.next(DocumentType.PAYMENT));
        payment.setBill(bill);
        payment.setPaymentDate(LocalDateTime.now(clock));
        payment.setAmount(amount);
        payment.setPaymentMode(mode);
        payment.setPaidBy(payer);
        payment.setTransactionNo(reference);
        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} of {} by {} on bill {}", saved.getPaymentNo(), amount, payer, bill.getBillNo());
        return saved;
    }

    private PaymentResponse toResponse(Payment payment) {
        List<RefundResponse> refunds = refundRepository.findByPaymentIdOrderByRefundDateAsc(payment.getId()).stream()
                .map(RefundResponse::from)
                .toList();
        return PaymentResponse.from(payment, refunds);
    }
}
