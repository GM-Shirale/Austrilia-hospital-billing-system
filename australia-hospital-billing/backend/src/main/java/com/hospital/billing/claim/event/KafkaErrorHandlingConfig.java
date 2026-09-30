package com.hospital.billing.claim.event;

import com.hospital.billing.common.exception.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Listener error handling (the Kafka equivalent of the REST GlobalExceptionHandler):
 * <ul>
 *   <li>transient errors: retried with exponential back-off 1s, 2s, 4s, 8s;</li>
 *   <li>then the record is published to "&lt;topic&gt;.DLT" for investigation / replay;</li>
 *   <li>poison messages (cannot be deserialised) and business-rule violations are NOT
 *       retried: they would fail identically every time.</li>
 * </ul>
 * Spring Boot automatically plugs a single CommonErrorHandler bean into the listener
 * container factory.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "kafka")
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> {
                    log.error("Sending record from {} (offset {}) to DLT: {}", record.topic(), record.offset(),
                            exception.getMessage());
                    return new TopicPartition(record.topic() + ".DLT", record.partition());
                });

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(4);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(DeserializationException.class, DomainException.class);
        return handler;
    }
}
