package ru.andreevcode.logicore.corelogistics.kafka.data;

import ru.andreevcode.logicore.corelogistics.outbox.data.OutboxEvent;

public record HubCapacityDepletedEvent(Long hubId, int remainingCapacity, String code) implements OutboxEvent {
}
