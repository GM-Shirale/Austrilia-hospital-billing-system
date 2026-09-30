package com.hospital.billing.claim.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Topics are created on startup by KafkaAdmin. 3 partitions allow 3 parallel consumers per
 * group; replication 1 suits the single-broker docker-compose (use 3 in production).
 * Dead-letter topics mirror the partition count because failed records keep their partition.
 */
@Configuration
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "kafka")
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    @Bean
    public KafkaAdmin.NewTopics claimTopics() {
        return new KafkaAdmin.NewTopics(
                topic(ClaimTopics.CLAIM_SUBMITTED),
                topic(ClaimTopics.CLAIM_ADJUDICATED),
                topic(ClaimTopics.CLAIM_REJECTED),
                topic(ClaimTopics.REMITTANCE_POSTED),
                topic(ClaimTopics.NOTIFICATION_DISPATCH),
                topic(ClaimTopics.CLAIM_SUBMITTED + ".DLT"),
                topic(ClaimTopics.CLAIM_ADJUDICATED + ".DLT"),
                topic(ClaimTopics.CLAIM_REJECTED + ".DLT"),
                topic(ClaimTopics.REMITTANCE_POSTED + ".DLT"),
                topic(ClaimTopics.NOTIFICATION_DISPATCH + ".DLT"));
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
