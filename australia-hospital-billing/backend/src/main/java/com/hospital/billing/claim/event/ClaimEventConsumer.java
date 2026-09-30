package com.hospital.billing.claim.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumers (profile "kafka"). Failures are retried with exponential back-off and then
 * sent to "<topic>.DLT" by the DefaultErrorHandler in {@link KafkaErrorHandlingConfig}.
 * Separate consumer groups let each workflow scale independently.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "kafka")
public class ClaimEventConsumer {

    private final ClaimEventRouter router;

    @KafkaListener(topics = ClaimTopics.CLAIM_SUBMITTED, groupId = "claims-submission", concurrency = "3")
    public void onClaimSubmitted(ClaimEvent event) {
        log.info("Consumed {} for claim {}", event.type(), event.reference());
        router.route(event);
    }

    @KafkaListener(topics = {ClaimTopics.CLAIM_ADJUDICATED, ClaimTopics.REMITTANCE_POSTED}, groupId = "claims-settlement")
    public void onClaimSettlement(ClaimEvent event) {
        log.info("Consumed {} for claim {}", event.type(), event.reference());
        router.route(event);
    }

    @KafkaListener(topics = ClaimTopics.CLAIM_REJECTED, groupId = "claims-denials")
    public void onClaimRejected(ClaimEvent event) {
        log.info("Consumed {} for claim {}", event.type(), event.reference());
        router.route(event);
    }

    @KafkaListener(topics = ClaimTopics.NOTIFICATION_DISPATCH, groupId = "notifications")
    public void onNotification(ClaimEvent event) {
        router.route(event);
    }
}
