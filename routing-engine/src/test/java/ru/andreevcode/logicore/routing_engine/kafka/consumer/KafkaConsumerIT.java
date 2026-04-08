package ru.andreevcode.logicore.routing_engine.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import ru.andreevcode.logicore.routing_engine.BaseIT;
import ru.andreevcode.logicore.routing_engine.kafka.data.HubCapacityDepletedEvent;
import ru.andreevcode.logicore.routing_engine.kafka.data.HubsEntity;
import ru.andreevcode.logicore.routing_engine.repo.HubsRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Slf4j
class KafkaConsumerIT extends BaseIT {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    HubsRepository hubsRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void shouldHandleDuplicatesAndOutOfOrder() {
        final String code = "MOW-1" + UUID.randomUUID();
        final long hubId = 101L;
        Instant t1 = Instant.parse("2026-01-12T10:04:00.02Z");
        Instant t0 = Instant.parse("2026-01-12T06:04:00.01Z");

        var uuid1 = UUID.randomUUID();
        log.info("uuid1 {}", uuid1);
        // event 1 T1
        sendEvent(code, new HubCapacityDepletedEvent(
                uuid1,
                t1,
                hubId,
                10,
                code
        ));

        // duplicate for event 1 T1 - should be skipped
        var uuid2 = UUID.randomUUID();
        log.info("uuid2 {}", uuid2);
        sendEvent(code, new HubCapacityDepletedEvent(
                uuid2,
                t1,
                hubId,
                10,
                code
        ));

        // older event 2 T0 - should be skipped
        var uuid3 = UUID.randomUUID();
        log.info("uuid3 {}", uuid3);
        sendEvent(code, new HubCapacityDepletedEvent(
                uuid3,
                t0,
                hubId,
                15,
                code
        ));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var hubOptional = hubsRepository.findById(hubId);
                    assertThat(hubOptional).isPresent();
                    assertThat(hubOptional).contains(new HubsEntity(hubId, code, 10, t1));
                });
    }

    private void sendEvent(String code, HubCapacityDepletedEvent event) {
        kafkaTemplate.send("logistics.hub.capacity.alerts", code, objectMapper.writeValueAsString(event));
    }

}