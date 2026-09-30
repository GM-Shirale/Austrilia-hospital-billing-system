package com.hospital.billing.pharmacy.domain;

import java.math.BigDecimal;

/**
 * GST treatment of a medicine or supply (A New Tax System (Goods and Services Tax) Act 1999):
 * <ul>
 *   <li>{@link #PBS} - PBS-listed medicine: GST-free;</li>
 *   <li>{@link #PRESCRIPTION} - prescription-only medicine supplied on a prescription: GST-free;</li>
 *   <li>{@link #OTC} - over-the-counter product: 10% GST;</li>
 *   <li>{@link #TAXABLE_SUPPLY} - other goods (e.g. hire, comfort items): 10% GST.</li>
 * </ul>
 * The rate is derived from the category so the catalogue cannot be misclassified.
 */
public enum SupplyCategory {
    PBS(BigDecimal.ZERO),
    PRESCRIPTION(BigDecimal.ZERO),
    OTC(new BigDecimal("0.1000")),
    TAXABLE_SUPPLY(new BigDecimal("0.1000"));

    private final BigDecimal gstRate;

    SupplyCategory(BigDecimal gstRate) {
        this.gstRate = gstRate;
    }

    public BigDecimal gstRate() {
        return gstRate;
    }

    public boolean gstFree() {
        return gstRate.signum() == 0;
    }
}
