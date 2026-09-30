package com.hospital.billing.claim.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default (no Kafka needed): publishes a Spring application event; {@link InMemoryClaimEventListener}
 * picks it up AFTER_COMMIT on an async thread, mimicking a broker.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryClaimEventPublisher implements ClaimEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(ClaimEvent event) {
        log.debug("Publishing {} for {} (in-memory)", event.type(), event.reference());
        applicationEventPublisher.publishEvent(event);
    }
}
