package com.hospital.billing.adjudication;

import com.hospital.billing.billing.domain.BillItemType;

import java.math.BigDecimal;

/**
 * One bill line as seen by the adjudicators.
 *
 * @param netAmount        amount charged for the line (incl. GST)
 * @param scheduleFeeTotal MBS schedule fee x quantity, or null if the line is not an MBS service
 */
public record AdjudicationLine(Long billItemId, BillItemType itemType, BigDecimal netAmount,
                               BigDecimal scheduleFeeTotal) {

    public boolean hasScheduleFee() {
        return scheduleFeeTotal != null && scheduleFeeTotal.signum() > 0;
    }
}
