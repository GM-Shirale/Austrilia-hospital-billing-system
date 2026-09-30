package com.hospital.billing.adjudication;

import com.hospital.billing.insurance.domain.PayerType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Strategy pattern (Open/Closed Principle): one implementation per payer. A new payer
 * (DVA, workers' compensation, overseas visitor cover) is a new @Component; neither the
 * AdjudicationEngine nor the billing/claim services change.
 *
 * <p>Payers are applied in {@code @Order} sequence (Medicare first, then the private fund).
 * Each strategy receives what is still unpaid on every line after the earlier payers.</p>
 */
public interface ClaimAdjudicatorStrategy {

    PayerType payerType();

    /** Whether this payer contributes at all for the given cover. */
    boolean supports(CoverageProfile coverage);

    /**
     * @param remainingByLine unpaid amount per bill item after earlier payers (read-only)
     */
    PayerAdjudication adjudicate(List<AdjudicationLine> lines, CoverageProfile coverage,
                                 Map<Long, BigDecimal> remainingByLine);
}
