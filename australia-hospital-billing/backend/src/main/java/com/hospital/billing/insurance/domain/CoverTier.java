package com.hospital.billing.insurance.domain;

import java.math.BigDecimal;

/**
 * Australian hospital cover tiers (Basic / Bronze / Silver / Gold, with "Plus" variants).
 * The real product rules are clinical-category based; here each tier is simplified into
 * the share of hospital charges (accommodation, nursing, theatre, equipment) and pharmacy
 * charges the fund pays. Values are illustrative.
 */
public enum CoverTier {
    BASIC("0.50", "0.00"),
    BRONZE("0.80", "0.50"),
    BRONZE_PLUS("0.90", "0.60"),
    SILVER("1.00", "0.70"),
    SILVER_PLUS("1.00", "0.80"),
    GOLD("1.00", "1.00");

    private final BigDecimal hospitalChargeCover;
    private final BigDecimal pharmacyCover;

    CoverTier(String hospitalChargeCover, String pharmacyCover) {
        this.hospitalChargeCover = new BigDecimal(hospitalChargeCover);
        this.pharmacyCover = new BigDecimal(pharmacyCover);
    }

    public BigDecimal hospitalChargeCover() {
        return hospitalChargeCover;
    }

    public BigDecimal pharmacyCover() {
        return pharmacyCover;
    }
}
