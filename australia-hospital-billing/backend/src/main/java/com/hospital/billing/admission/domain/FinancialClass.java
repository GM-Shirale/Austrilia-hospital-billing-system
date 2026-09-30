package com.hospital.billing.admission.domain;

/**
 * Patient election on admission. A PUBLIC patient in a public hospital is treated free of
 * charge under Medicare; a PRIVATE patient elects to be billed (and claims Medicare 75% of
 * the MBS fee plus their private fund).
 */
public enum FinancialClass {
    PUBLIC,
    PRIVATE
}
