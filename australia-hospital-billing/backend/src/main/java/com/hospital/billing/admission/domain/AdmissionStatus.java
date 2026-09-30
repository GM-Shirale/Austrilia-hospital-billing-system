package com.hospital.billing.admission.domain;

import java.util.Set;

public enum AdmissionStatus {
    ADMITTED,
    DISCHARGED,
    CANCELLED;

    public boolean canTransitionTo(AdmissionStatus target) {
        return this == ADMITTED && Set.of(DISCHARGED, CANCELLED).contains(target);
    }
}
