package com.hospital.billing.adjudication;

import com.hospital.billing.common.money.Money;
import com.hospital.billing.insurance.domain.PayerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single Responsibility: this class ONLY splits money between payers. It knows nothing about
 * JPA, HTTP, Kafka or claims; it is pure and deterministic, which makes it trivially
 * unit-testable (see AdjudicationEngineTest).
 *
 * <p>Algorithm: start with "remaining = net amount" per line; apply each applicable payer
 * strategy in order; every payer can pay at most what is still remaining (the engine enforces
 * this even if a strategy misbehaves). Whatever remains is the patient's out-of-pocket gap.</p>
 */
@Component
@RequiredArgsConstructor
public class AdjudicationEngine {

    private final AdjudicatorResolver resolver;

    public AdjudicationResult adjudicate(List<AdjudicationLine> lines, CoverageProfile coverage) {
        Map<Long, BigDecimal> remaining = new LinkedHashMap<>();
        lines.forEach(line -> remaining.put(line.billItemId(), Money.of(line.netAmount())));

        Map<PayerType, Map<Long, BigDecimal>> paidByPayer = new EnumMap<>(PayerType.class);
        List<PayerAdjudication> payerResults = new ArrayList<>();

        for (ClaimAdjudicatorStrategy strategy : resolver.strategiesFor(coverage)) {
            PayerAdjudication proposed = strategy.adjudicate(lines, coverage, Collections.unmodifiableMap(remaining));
            Map<Long, BigDecimal> applied = new HashMap<>();
            List<LineBenefit> accepted = new ArrayList<>();
            for (LineBenefit benefit : proposed.lineBenefits()) {
                BigDecimal open = remaining.getOrDefault(benefit.billItemId(), Money.ZERO);
                BigDecimal amount = Money.min(Money.nonNegative(benefit.amount()), open);
                if (amount.signum() == 0) {
                    continue;
                }
                remaining.put(benefit.billItemId(), Money.of(open.subtract(amount)));
                applied.merge(benefit.billItemId(), amount, BigDecimal::add);
                accepted.add(new LineBenefit(benefit.billItemId(), amount, benefit.note()));
            }
            paidByPayer.put(strategy.payerType(), applied);
            payerResults.add(new PayerAdjudication(strategy.payerType(), accepted, proposed.notes()));
        }

        Map<Long, BigDecimal> medicare = paidByPayer.getOrDefault(PayerType.MEDICARE, Map.of());
        Map<Long, BigDecimal> fund = paidByPayer.getOrDefault(PayerType.PRIVATE_FUND, Map.of());
        List<LineOutcome> outcomes = lines.stream()
                .map(line -> new LineOutcome(line.billItemId(),
                        Money.of(medicare.getOrDefault(line.billItemId(), Money.ZERO)),
                        Money.of(fund.getOrDefault(line.billItemId(), Money.ZERO)),
                        Money.of(remaining.get(line.billItemId()))))
                .toList();
        return new AdjudicationResult(outcomes, payerResults);
    }
}
