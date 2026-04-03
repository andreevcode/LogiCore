package ru.andreevcode.logicore.corelogistics.kafka.data;

import ru.andreevcode.logicore.corelogistics.outbox.data.OutboxEvent;

import java.time.Instant;
import java.util.UUID;

public record HubCapacityDepletedEvent(UUID eventId,
                                       Instant createdAt,
                                       Long hubId,
                                       int remainingCapacity,
                                       String code) implements OutboxEvent {
}
