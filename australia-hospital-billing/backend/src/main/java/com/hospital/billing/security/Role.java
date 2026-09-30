package com.hospital.billing.security;

public enum Role {
    ADMIN,
    BILLING_OFFICER,
    DOCTOR,
    RECEPTIONIST,
    LAB,
    PHARMACY;

    /** Spring Security authority name, e.g. ROLE_BILLING (what hasRole('BILLING_OFFICER') checks). */
    public String authority() {
        return "ROLE_" + name();
    }
}
