package com.hospital.billing.insurance.domain;

public enum PayerType {
    /** Medicare Benefits Schedule, paid by Services Australia (claimed via ECLIPSE). */
    MEDICARE,
    /** Private health insurance fund (Bupa, Medibank, HCF ...). */
    PRIVATE_FUND
}
