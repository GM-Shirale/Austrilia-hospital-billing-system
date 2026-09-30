package com.hospital.billing.billing.charge;

import com.hospital.billing.admission.domain.Admission;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Open/Closed Principle: each clinical module contributes charges through its own collector.
 * Adding e.g. theatre or nursing charges means adding a new @Component, without touching
 * BillingServiceImpl (Spring injects every ChargeCollector as a List).
 */
public interface ChargeCollector {

    List<ChargeLine> collect(Admission admission, LocalDateTime asOf);
}
