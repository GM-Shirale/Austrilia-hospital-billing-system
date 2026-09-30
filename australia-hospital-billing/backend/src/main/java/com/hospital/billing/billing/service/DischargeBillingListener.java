package com.hospital.billing.billing.service;

import com.hospital.billing.admission.event.AdmissionDischargedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Discharge -> bill. A plain (synchronous) {@link EventListener} runs inside the discharge
 * transaction: if costing fails, the discharge rolls back too, so a patient is never
 * discharged with a half-built bill.
 */
@Component
@RequiredArgsConstructor
public class DischargeBillingListener {

    private final BillingService billingService;

    @EventListener
    public void onDischarge(AdmissionDischargedEvent event) {
        billingService.prepareDischargeBill(event.admissionId(), event.dischargedAt());
    }
}
