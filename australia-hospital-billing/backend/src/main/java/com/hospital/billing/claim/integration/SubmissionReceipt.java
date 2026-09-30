package com.hospital.billing.claim.integration;

import java.time.Instant;

public record SubmissionReceipt(String payerReference, Instant receivedAt) {
}
