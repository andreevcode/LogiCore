package ru.andreevcode.logicore.routing_engine.kafka.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class HubsEntity {
    private Long id;
    private String code;
    private Integer capacity;
    private Instant capacityUpdatedAt;
}
