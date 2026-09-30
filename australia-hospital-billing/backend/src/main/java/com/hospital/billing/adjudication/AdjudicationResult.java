package com.hospital.billing.adjudication;

import com.hospital.billing.common.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record AdjudicationResult(List<LineOutcome> lines, List<PayerAdjudication> payers) {

    public Map<Long, LineOutcome> byBillItemId() {
        return lines.stream().collect(Collectors.toMap(LineOutcome::billItemId, Function.identity()));
    }

    public BigDecimal totalMedicare() {
        return sum(LineOutcome::medicare);
    }

    public BigDecimal totalPrivateFund() {
        return sum(LineOutcome::privateFund);
    }

    public BigDecimal totalPatientGap() {
        return sum(LineOutcome::patientGap);
    }

    public List<String> notes() {
        return payers.stream().flatMap(p -> p.notes().stream()).toList();
    }

    private BigDecimal sum(Function<LineOutcome, BigDecimal> field) {
        return Money.of(lines.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
