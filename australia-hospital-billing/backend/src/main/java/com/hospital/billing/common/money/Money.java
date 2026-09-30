package com.hospital.billing.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * AUD money helpers. All amounts are BigDecimal with scale 2 and HALF_UP rounding
 * (never double: 0.1 + 0.2 != 0.3 in floating point).
 */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal HUNDRED = new BigDecimal("100");

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String value) {
        return of(new BigDecimal(value));
    }

    /** amount x rate, e.g. percentOf(100.00, 0.75) = 75.00 */
    public static BigDecimal percentOf(BigDecimal amount, BigDecimal rate) {
        return of(of(amount).multiply(rate));
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? ZERO : of(value);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
