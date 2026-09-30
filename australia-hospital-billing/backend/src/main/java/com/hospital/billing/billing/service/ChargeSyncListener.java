package com.hospital.billing.billing.service;

import com.hospital.billing.common.event.ChargeSourceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Clinical modules publish {@link ChargeSourceChangedEvent}; this listener re-collects the
 * admission's DRAFT bill in the same transaction, so a lab order, a dispensed medicine or a
 * consultation shows up on the bill immediately (with MBS item, GST and rebates).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargeSyncListener {

    private final BillingService billingService;

    @EventListener
    public void onChargeSourceChanged(ChargeSourceChangedEvent event) {
        log.debug("Charge source changed on admission {}: {}", event.admissionId(), event.reason());
        billingService.syncDraftBill(event.admissionId());
    }
}
