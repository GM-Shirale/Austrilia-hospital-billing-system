package com.hospital.billing.claim.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * In-memory stand-in for the Kafka consumers: runs AFTER the publishing transaction commits,
 * on a separate thread (@Async), exactly like a broker consumer would.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryClaimEventListener {

    private final ClaimEventRouter router;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onClaimEvent(ClaimEvent event) {
        try {
            router.route(event);
        } catch (RuntimeException ex) {
            log.error("Failed to process {} for {}", event.type(), event.reference(), ex);
        }
    }
}
