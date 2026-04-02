package ru.andreevcode.logicore.corelogistics.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import ru.andreevcode.logicore.corelogistics.kafka.producer.KafkaProducer;
import ru.andreevcode.logicore.corelogistics.outbox.data.OutboxEntity;
import ru.andreevcode.logicore.corelogistics.repo.OutboxRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {
    @Value(value = "${app.outbox.batch.size:50}")
    private int outboxProcessBatch;

    private final KafkaProducer<String, String> kafkaProducer;
    private final OutboxRepository outboxRepository;
    private final OutboxEventsMarker outboxEventsMarker;
    private final TransactionTemplate transactionTemplate;


    @Scheduled(fixedDelayString = "${app.outbox.delay:1000}")
    public void relayEvents() {
        List<OutboxEntity> entities = transactionTemplate.execute(status -> claimBatch());
        if (entities.isEmpty()) {
            return;
        }

        relay(entities);

        transactionTemplate.executeWithoutResult(status -> finalizeBatch(entities));
    }

    private List<OutboxEntity> claimBatch() {
        List<OutboxEntity> entities = outboxRepository.findToProcessTop(outboxProcessBatch);

        if (entities.isEmpty()) {
            return List.of();
        }
        var tsNow = Instant.now();
        entities.forEach(entity -> {
            entity.setEventStatus(OutboxEventStatus.PROCESSING);
            entity.setUpdatedAt(tsNow);
        });

        outboxRepository.batchUpdate(entities);
        return entities;
    }

    private void relay(List<OutboxEntity> entities) {
        Map<OutboxEventType, List<OutboxEntity>> eventsToProcess = entities.stream()
                .collect(Collectors.groupingBy(
                        OutboxEntity::getEventType,
                        Collectors.collectingAndThen(Collectors.toList(), outboxEventsMarker::markObsoleteSkipped)
                ));
        kafkaProducer.sendMessages(eventsToProcess);
    }


    private void finalizeBatch(List<OutboxEntity> entities) {
        outboxRepository.batchUpdate(entities);
    }
}
