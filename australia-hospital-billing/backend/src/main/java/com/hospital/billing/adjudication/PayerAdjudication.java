package com.hospital.billing.adjudication;

import com.hospital.billing.common.money.Money;
import com.hospital.billing.insurance.domain.PayerType;

import java.math.BigDecimal;
import java.util.List;

/** What one payer (Medicare or a private fund) pays, line by line. */
public record PayerAdjudication(PayerType payerType, List<LineBenefit> lineBenefits, List<String> notes) {

    public BigDecimal total() {
        return Money.of(lineBenefits.stream().map(LineBenefit::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
