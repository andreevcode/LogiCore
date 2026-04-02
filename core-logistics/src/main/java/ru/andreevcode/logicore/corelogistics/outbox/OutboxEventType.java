package ru.andreevcode.logicore.corelogistics.outbox;

import lombok.Getter;

@Getter
public enum OutboxEventType {
    HUB_CAPACITY_DEPLETED("spring.kafka.topic.hub.capacity.alerts.name");

    private final String topicPropertyKey;

    OutboxEventType(String topicPropertyKey) {
        this.topicPropertyKey = topicPropertyKey;
    }
}
