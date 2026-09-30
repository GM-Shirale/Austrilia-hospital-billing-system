package com.hospital.billing.lab.domain;

import java.util.Map;
import java.util.Set;

public enum LabOrderStatus {
    ORDERED,
    COLLECTED,
    COMPLETED,
    CANCELLED;

    private static final Map<LabOrderStatus, Set<LabOrderStatus>> ALLOWED = Map.of(
            ORDERED, Set.of(COLLECTED, CANCELLED),
            COLLECTED, Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of());

    public boolean canTransitionTo(LabOrderStatus target) {
        return ALLOWED.get(this).contains(target);
    }
}
