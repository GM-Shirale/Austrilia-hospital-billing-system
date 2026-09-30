package com.hospital.billing.claim.event;

/**
 * Dependency Inversion: business services publish through this abstraction and do not know
 * whether Kafka ({@link ClaimEventProducer}) or in-process Spring events
 * ({@link InMemoryClaimEventPublisher}) deliver the message.
 */
public interface ClaimEventPublisher {

    /** Publishes after the current transaction commits, so consumers never see uncommitted state. */
    void publish(ClaimEvent event);
}
