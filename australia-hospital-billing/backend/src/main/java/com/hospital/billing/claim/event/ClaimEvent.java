package com.hospital.billing.claim.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable integration event. It always carries the tenant id, because consumers run on
 * other threads (or other JVMs) where no HTTP request / TenantContext exists.
 *
 * @param eventId   unique id, lets consumers detect duplicates (Kafka is at-least-once)
 * @param claimId   null for events that are not about a claim (e.g. bill notifications)
 * @param reference human-readable document number (claim no / bill no)
 */
public record ClaimEvent(UUID eventId,
                         ClaimEventType type,
                         UUID tenantId,
                         Long claimId,
                         String reference,
                         BigDecimal amount,
                         String message,
                         Instant occurredAt) {

    public static ClaimEvent of(ClaimEventType type, UUID tenantId, Long claimId, String reference,
                                BigDecimal amount, String message) {
        return new ClaimEvent(UUID.randomUUID(), type, tenantId, claimId, reference, amount, message, Instant.now());
    }

    /** Kafka key: all events of one claim land on the same partition, so they stay in order. */
    public String partitionKey() {
        return claimId == null ? tenantId + ":" + reference : tenantId + ":" + claimId;
    }
}
