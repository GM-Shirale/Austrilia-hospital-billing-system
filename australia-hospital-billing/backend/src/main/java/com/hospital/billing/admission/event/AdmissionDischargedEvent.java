package com.hospital.billing.admission.event;

import java.time.LocalDateTime;

/**
 * Published inside the discharge transaction. Billing listens synchronously so the stay is
 * costed and the bill lines are locked atomically with the discharge.
 */
public record AdmissionDischargedEvent(Long admissionId, LocalDateTime dischargedAt) {
}
