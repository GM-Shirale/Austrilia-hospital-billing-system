package com.hospital.billing.claim.event;

public enum ClaimEventType {
    CLAIM_SUBMITTED(ClaimTopics.CLAIM_SUBMITTED),
    CLAIM_ADJUDICATED(ClaimTopics.CLAIM_ADJUDICATED),
    CLAIM_REJECTED(ClaimTopics.CLAIM_REJECTED),
    REMITTANCE_POSTED(ClaimTopics.REMITTANCE_POSTED),
    NOTIFICATION(ClaimTopics.NOTIFICATION_DISPATCH);

    private final String topic;

    ClaimEventType(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }
}
