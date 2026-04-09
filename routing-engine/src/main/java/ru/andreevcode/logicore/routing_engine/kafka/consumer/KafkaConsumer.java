package ru.andreevcode.logicore.routing_engine.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import ru.andreevcode.logicore.routing_engine.kafka.data.HubCapacityDepletedEvent;
import ru.andreevcode.logicore.routing_engine.kafka.exception.BadFormatKafkaConsumerException;
import ru.andreevcode.logicore.routing_engine.service.HubsService;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final HubsService hubsService;

    private final ObjectMapper objectMapper;

    @KafkaListener(
            id = "hub-cap-depleted-consumer",
            topics = "${spring.kafka.topic.hub.capacity.alerts.name}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2),
            autoCreateTopics = "true",
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
            exclude = {BadFormatKafkaConsumerException.class}
    )
    public void listen(String eventString) {
        HubCapacityDepletedEvent event = objectMapper.readValue(eventString, HubCapacityDepletedEvent.class);

        int rowsUpdated = hubsService.updateCapacity(event);
        if (rowsUpdated > 0) {
            log.info("SAVED event: uuid={}, hubId={}, capacity={}, ts={}",
                    event.eventId(), event.hubId(), event.remainingCapacity(), event.createdAt());
        } else {
            log.info("SKIPPED event: event with uuid={}, hubId={}, capacity={}, ts={}",
                    event.eventId(), event.hubId(), event.remainingCapacity(), event.createdAt());
        }
    }

    @DltHandler
    public void handleDltEvent(String eventString,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        log.error("DLT reached! Topic: {}, Payload: {}, Reason: {}", topic, eventString, errorMessage);
        // Далее, например, сохранение в dead_letter в БД
    }
}
