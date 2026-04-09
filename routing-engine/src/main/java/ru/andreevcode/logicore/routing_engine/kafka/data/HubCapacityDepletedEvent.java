package ru.andreevcode.logicore.routing_engine.kafka.data;


import java.time.Instant;
import java.util.UUID;

public record HubCapacityDepletedEvent(UUID eventId,
                                       Instant createdAt,
                                       Long hubId,
                                       int remainingCapacity,
                                       String code) {
}
