package com.hospital.billing.billing.domain;

import com.hospital.billing.insurance.domain.PayerType;

public enum PaidBy {
    PATIENT,
    MEDICARE,
    PRIVATE_FUND;

    public static PaidBy from(PayerType payerType) {
        return switch (payerType) {
            case MEDICARE -> MEDICARE;
            case PRIVATE_FUND -> PRIVATE_FUND;
        };
    }
}
