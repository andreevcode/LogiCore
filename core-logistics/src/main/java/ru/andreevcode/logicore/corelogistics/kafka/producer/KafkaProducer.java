package ru.andreevcode.logicore.corelogistics.kafka.producer;

import org.springframework.kafka.support.SendResult;
import ru.andreevcode.logicore.corelogistics.outbox.OutboxEventType;
import ru.andreevcode.logicore.corelogistics.outbox.data.OutboxEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface KafkaProducer<K, V> {
    CompletableFuture<SendResult<K, V>> sendMessage(OutboxEntity entity, String topic, K key, V value);

    void sendMessages(Map<OutboxEventType, List<OutboxEntity>> entitiesToProcess);
}
