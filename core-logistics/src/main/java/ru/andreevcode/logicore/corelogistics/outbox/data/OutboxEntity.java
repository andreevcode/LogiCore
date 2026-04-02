package ru.andreevcode.logicore.corelogistics.outbox.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.andreevcode.logicore.corelogistics.outbox.OutboxEventStatus;
import ru.andreevcode.logicore.corelogistics.outbox.OutboxEventType;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class OutboxEntity {
    private Long id;
    private UUID externalId;
    private OutboxEventType eventType;
    private String aggregationId;
    private String payload;
    private OutboxEventStatus eventStatus;
    private Instant createdAt;
    private Instant updatedAt;
    private int retryCount;
    private String lastError;
    private String traceId;
}