package com.hospital.billing.adjudication;

import com.hospital.billing.insurance.domain.PayerType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy resolver / factory. Spring injects every {@link ClaimAdjudicatorStrategy} bean,
 * already sorted by {@code @Order}; this class indexes them by payer type. Registering a
 * new payer needs zero changes here.
 */
@Component
public class AdjudicatorResolver {

    private final List<ClaimAdjudicatorStrategy> orderedStrategies;
    private final Map<PayerType, ClaimAdjudicatorStrategy> strategiesByPayer;

    public AdjudicatorResolver(List<ClaimAdjudicatorStrategy> strategies) {
        this.orderedStrategies = List.copyOf(strategies);
        Map<PayerType, ClaimAdjudicatorStrategy> byPayer = new EnumMap<>(PayerType.class);
        for (ClaimAdjudicatorStrategy strategy : strategies) {
            if (byPayer.putIfAbsent(strategy.payerType(), strategy) != null) {
                throw new IllegalStateException("More than one adjudicator registered for " + strategy.payerType());
            }
        }
        this.strategiesByPayer = Collections.unmodifiableMap(byPayer);
    }

    /** Payers that contribute for this cover, in payment order. */
    public List<ClaimAdjudicatorStrategy> strategiesFor(CoverageProfile coverage) {
        return orderedStrategies.stream().filter(s -> s.supports(coverage)).toList();
    }

    public ClaimAdjudicatorStrategy forPayer(PayerType payerType) {
        ClaimAdjudicatorStrategy strategy = strategiesByPayer.get(payerType);
        if (strategy == null) {
            throw new IllegalStateException("No adjudicator registered for " + payerType);
        }
        return strategy;
    }
}
