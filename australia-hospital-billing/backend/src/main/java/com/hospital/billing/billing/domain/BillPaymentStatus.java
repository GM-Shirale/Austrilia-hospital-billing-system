package com.hospital.billing.billing.domain;

import java.math.BigDecimal;

/**
 * Settlement of the PATIENT's share of a bill, as printed on the tax invoice.
 * (BillStatus tracks the whole bill incl. Medicare / fund remittances.)
 */
public enum BillPaymentStatus {
    PENDING,
    PARTIAL,
    FULL;

    public static BillPaymentStatus of(BillStatus billStatus, BigDecimal patientPayable, BigDecimal patientPaid) {
        if (billStatus == BillStatus.DRAFT || billStatus == BillStatus.CANCELLED) {
            return PENDING;
        }
        if (patientPaid.compareTo(patientPayable) >= 0) {
            return FULL;                // includes a fully-funded (zero gap) public patient
        }
        return patientPaid.signum() > 0 ? PARTIAL : PENDING;
    }
}
