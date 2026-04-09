package ru.andreevcode.logicore.routing_engine.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import ru.andreevcode.logicore.routing_engine.BaseIT;
import ru.andreevcode.logicore.routing_engine.kafka.data.HubCapacityDepletedEvent;
import ru.andreevcode.logicore.routing_engine.kafka.data.HubsEntity;
import ru.andreevcode.logicore.routing_engine.repo.HubsRepository;
import ru.andreevcode.logicore.routing_engine.service.HubsService;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Slf4j
class KafkaConsumerIT extends BaseIT {
    private static final String DLT_SUFFIX = "-dlt";

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @MockitoSpyBean
    HubsRepository hubsRepository;

    @MockitoSpyBean
    HubsService hubsService;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoSpyBean
    KafkaConsumer kafkaConsumer;

    @Value("${spring.kafka.topic.hub.capacity.alerts.name}")
    private String capacityAlertsTopic;

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


    @Test
    void shouldHandleEventViaRetryTopicsOnException() {
        final String code1 = "MOW-1" + UUID.randomUUID();
        final String code2 = "MOW-2" + UUID.randomUUID();
        final long hubId1 = 111L;
        final long hubId2 = 112L;

        Instant t1 = Instant.parse("2026-01-13T10:00:00.04Z");
        Instant t2 = Instant.parse("2026-01-13T10:02:00.15Z");

        var hubsEntity1 = new HubsEntity(hubId1, code1, 20, t1);
        var hubsEntity2 = new HubsEntity(hubId2, code2, 30, t2);

        //The first event is failed 2 times
        doThrow(new DataAccessResourceFailureException("pg connection is lost"))
                .doThrow(new DataAccessResourceFailureException("pg connection is lost"))
                .doCallRealMethod() // Чтобы в третий раз данные реально записались в БД
                .when(hubsRepository).upsertCapacity(hubsEntity1);

        // event 1 T1
        sendEvent(code1, new HubCapacityDepletedEvent(
                UUID.randomUUID(),
                t1,
                hubId1,
                20,
                code1
        ));

        // event 2 T2
        sendEvent(code1, new HubCapacityDepletedEvent(
                UUID.randomUUID(),
                t2,
                hubId2,
                30,
                code2
        ));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var hubOptional1 = hubsRepository.findById(hubId1);
                    assertThat(hubOptional1).isPresent();
                    assertThat(hubOptional1).contains(hubsEntity1);

                    var hubOptional2 = hubsRepository.findById(hubId2);
                    assertThat(hubOptional2).isPresent();
                    assertThat(hubOptional2).contains(hubsEntity2);
                });

        verify(hubsRepository, times(3)).upsertCapacity(hubsEntity1);
        verify(hubsRepository, times(1)).upsertCapacity(hubsEntity2);
    }

    @Test
    void shouldHandleDltEventAfterAllAttempts() {
        final String code1 = "MOW-1" + UUID.randomUUID();
        final long hubId1 = 121L;

        Instant t1 = Instant.parse("2026-01-14T10:00:00.04Z");

        var hubsEntity1 = new HubsEntity(hubId1, code1, 20, t1);
        var event1 = new HubCapacityDepletedEvent(
                UUID.randomUUID(),
                t1,
                hubId1,
                20,
                code1
        );

        //All available attempts are failed
        doThrow(new DataAccessResourceFailureException("pg connection is lost"))
                .when(hubsRepository).upsertCapacity(hubsEntity1);

        sendEvent(code1, event1);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(hubsRepository, times(3)).upsertCapacity(hubsEntity1);
                    verify(kafkaConsumer, times(1)).handleDltEvent(
                            objectMapper.writeValueAsString(event1),
                            capacityAlertsTopic + DLT_SUFFIX,
                            "Listener failed; pg connection is lost"
                    );
                });
    }

    @Test
    void shouldHandleStraightDltOnPoisonPillEvent() {
        final String code1 = "MOW-1" + UUID.randomUUID();
        final long hubId1 = -121L; // poison pill
        Instant t1 = Instant.parse("2026-01-14T10:00:00.04Z");
        var event1 = new HubCapacityDepletedEvent(UUID.randomUUID(), t1, hubId1, 20, code1);

        sendEvent(code1, event1);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(hubsService, times(1)).updateCapacity(any());
                    verify(hubsRepository, times(0)).upsertCapacity(any());
                    verify(kafkaConsumer, times(1)).handleDltEvent(
                            eq(objectMapper.writeValueAsString(event1)),
                            eq(capacityAlertsTopic + DLT_SUFFIX),
                            anyString()
                    );
                });
    }

    private void sendEvent(String code, HubCapacityDepletedEvent event) {
        kafkaTemplate.send(capacityAlertsTopic, code, objectMapper.writeValueAsString(event));
    }

}