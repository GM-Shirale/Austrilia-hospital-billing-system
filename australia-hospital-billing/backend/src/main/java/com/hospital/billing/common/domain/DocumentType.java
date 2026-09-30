package com.hospital.billing.common.domain;

public enum DocumentType {
    PATIENT("MRN"),
    ADMISSION("ADM"),
    CONSULTATION("CON"),
    LAB_ORDER("LAB"),
    PRESCRIPTION("RX"),
    BILL("BILL"),
    PAYMENT("PAY"),
    REFUND("RFD"),
    CLAIM("CLM"),
    EOB("EOB");

    private final String prefix;

    DocumentType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
