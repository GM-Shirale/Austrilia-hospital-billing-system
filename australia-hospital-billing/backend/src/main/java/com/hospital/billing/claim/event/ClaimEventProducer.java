package com.hospital.billing.claim.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Kafka producer. Sends are deferred to afterCommit(): if the DB transaction rolls back, no
 * event is emitted, and consumers never race ahead of the committed claim state.
 *
 * <p>Trade-off: a crash between commit and send loses the event. The production-grade fix is
 * the Transactional Outbox pattern (write the event to an outbox table in the same
 * transaction and relay it with Debezium or a poller); noted as a next step in the README.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "kafka")
public class ClaimEventProducer implements ClaimEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(ClaimEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    private void send(ClaimEvent event) {
        kafkaTemplate.send(event.type().topic(), event.partitionKey(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for {} to Kafka", event.type(), event.reference(), ex);
                    } else {
                        log.info("Published {} for {} to {}-{}@{}", event.type(), event.reference(),
                                result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
