package com.hospital.billing.common.event;

/** A bill was finalised; the claims module creates the Medicare / fund claims after commit. */
public record BillFinalizedEvent(Long billId, String billNo) {
}
