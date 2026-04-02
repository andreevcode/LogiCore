package ru.andreevcode.logicore.corelogistics.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.andreevcode.logicore.corelogistics.outbox.OutboxEventStatus;
import ru.andreevcode.logicore.corelogistics.outbox.OutboxEventType;
import ru.andreevcode.logicore.corelogistics.outbox.data.OutboxEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaOutboxProducer implements KafkaProducer<String, String> {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Environment env;

    @Override
    public CompletableFuture<SendResult<String, String>> sendMessage(OutboxEntity entity, String topic,
                                                                     String key, String value) {
        return kafkaTemplate.send(topic, key, value)
                .handle((result, ex) -> {
                    var tsNow = Instant.now();
                    if (ex == null) {
                        entity.setEventStatus(OutboxEventStatus.SENT);
                        entity.setUpdatedAt(tsNow);
                        log.info("Sent message [{}] with offset=[{}]", value, result.getRecordMetadata().offset());
                        return result;
                    } else {
                        entity.setEventStatus(OutboxEventStatus.FAILED);
                        entity.setLastError(ex.getMessage());
                        entity.setUpdatedAt(tsNow);
                        log.error("Unable to send message=[{}] due to + {}", value, ex.getMessage());
                        // to prevent exception propagation and skipping sending other messages
                        // if only one message is crushed
                        return null;
                    }
                });
    }

    @Override
    public void sendMessages(Map<OutboxEventType, List<OutboxEntity>> entitiesToProcess) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        entitiesToProcess.forEach((type, entityGroup) -> {
            String topic = env.getProperty(type.getTopicPropertyKey());
            entityGroup.forEach(entity -> {
                if (entity.getEventStatus() == OutboxEventStatus.SKIPPED) {
                    return;
                }
                String key = entity.getAggregationId();
                String value = entity.getPayload();
                futures.add(sendMessage(entity, topic, key, value));
            });
        });

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
