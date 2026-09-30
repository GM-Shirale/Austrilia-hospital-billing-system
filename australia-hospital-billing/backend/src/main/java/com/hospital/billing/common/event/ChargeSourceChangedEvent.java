package com.hospital.billing.common.event;

/**
 * Something billable changed on an admission (lab order placed or cancelled, medicine
 * dispensed, consultation recorded, bed moved...). Billing listens and re-syncs the admission's
 * DRAFT bill so the bill always mirrors the clinical record - clinical modules never call
 * billing directly.
 */
public record ChargeSourceChangedEvent(Long admissionId, String reason) {
}
