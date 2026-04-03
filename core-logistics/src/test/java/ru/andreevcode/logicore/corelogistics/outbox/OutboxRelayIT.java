package ru.andreevcode.logicore.corelogistics.outbox;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ru.andreevcode.logicore.corelogistics.BaseIT;
import ru.andreevcode.logicore.corelogistics.kafka.data.HubCapacityDepletedEvent;
import ru.andreevcode.logicore.corelogistics.outbox.data.OutboxEntity;
import ru.andreevcode.logicore.corelogistics.repo.OutboxRepository;
import ru.andreevcode.logicore.corelogistics.service.TransportHubService;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.andreevcode.logicore.corelogistics.outbox.OutboxEventStatus.*;
import static ru.andreevcode.logicore.corelogistics.repo.OutboxRepository.OUTBOX_ROW_MAPPER;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class OutboxRelayIT extends BaseIT {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    OutboxRelay outboxRelay;

    @TestConfiguration
    static class KafkaTestConfig {
        @Bean
        public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf) {
            return new KafkaTemplate<>(pf);
        }
    }

    @MockitoSpyBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("TRUNCATE TABLE logistics.transport_hub, logistics.outbox RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldSkipObsoleteAndSendLastHubCapacityDepletedEvent() {
        List<OutboxEntity> entities = prepareBaseOutbox(1);

        // Настраиваем консьюмер вручную, чтобы гарантировать чтение с начала (earliest)
        // Используем адрес брокера прямо из контейнера
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(),
                "test-group-" + UUID.randomUUID(), // Уникальная группа для каждого теста
                false
        );
        consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
                new StringDeserializer());

        try (Consumer<String, String> testConsumer = consumerFactory.createConsumer()) {
            testConsumer.subscribe(Collections.singleton("logistics.hub.capacity.alerts"));

            outboxRelay.relayEvents();
            List<OutboxEntity> updated = jdbcTemplate.query("select * from logistics.outbox", OUTBOX_ROW_MAPPER);
            assertThat(updated)
                    .hasSize(4)
                    .extracting(OutboxEntity::getEventStatus, OutboxEntity::getExternalId)
                    .containsExactly(
                            tuple(SKIPPED, entities.get(0).getExternalId()),
                            tuple(SKIPPED, entities.get(1).getExternalId()),
                            tuple(SENT, entities.get(2).getExternalId()),
                            tuple(SENT, entities.get(3).getExternalId())
                    );

            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofSeconds(5));
            assertThat(records)
                    .filteredOn(rec -> List.of("hub-test-1", "hub-test-2").contains(rec.key()))
                    .extracting(ConsumerRecord::key, rec ->
                            objectMapper.readValue(rec.value(), HubCapacityDepletedEvent.class))
                    .hasSize(2)
                    .containsExactlyInAnyOrder(
                            tuple("hub-test-1", objectMapper.readValue(entities.get(2).getPayload(), HubCapacityDepletedEvent.class)),
                            tuple("hub-test-2", objectMapper.readValue(entities.get(3).getPayload(), HubCapacityDepletedEvent.class))
                    );
        }
    }


    @Test
    void shouldGoFailedOnPartlyKafkaSendingException(){
        List<OutboxEntity> entities = prepareBaseOutbox(3)    ;

        // Настройка ошибки: для этого сообщения KafkaTemplate вернет исключение
        CompletableFuture<SendResult<String, String>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("Kafka connection lost"));

        doReturn(failedFuture).when(kafkaTemplate).send(eq("logistics.hub.capacity.alerts"), eq("hub-test-3"), anyString());


        // Настраиваем консьюмер вручную, чтобы гарантировать чтение с начала (earliest)
        // Используем адрес брокера прямо из контейнера
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(),
                "test-group-" + UUID.randomUUID(), // Уникальная группа для каждого теста
                false
        );
        consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
                new StringDeserializer());

        try (Consumer<String, String> testConsumer = consumerFactory.createConsumer()) {
            testConsumer.subscribe(Collections.singleton("logistics.hub.capacity.alerts"));

            outboxRelay.relayEvents();
            List<OutboxEntity> updated = jdbcTemplate.query("select * from logistics.outbox", OUTBOX_ROW_MAPPER);
            assertThat(updated)
                    .hasSize(4)
                    .extracting(OutboxEntity::getEventStatus, OutboxEntity::getExternalId)
                    .containsExactly(
                            tuple(SKIPPED, entities.get(0).getExternalId()),
                            tuple(SKIPPED, entities.get(1).getExternalId()),
                            tuple(FAILED, entities.get(2).getExternalId()),
                            tuple(SENT, entities.get(3).getExternalId())
                    );

            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofSeconds(5));
            assertThat(records)
                    .filteredOn(rec -> List.of("hub-test-3", "hub-test-4").contains(rec.key()))
                    .extracting(ConsumerRecord::key, rec ->
                            objectMapper.readValue(rec.value(), HubCapacityDepletedEvent.class))
                    .hasSize(1)
                    .containsExactlyInAnyOrder(
                            tuple("hub-test-4", objectMapper.readValue(entities.get(3).getPayload(), HubCapacityDepletedEvent.class))
                    );
        }

    }

    private List<OutboxEntity> prepareBaseOutbox(int seed){
        var ts1 = Instant.now().minus(2, ChronoUnit.HOURS);
        var ts2 = Instant.now().minus(1, ChronoUnit.HOURS);
        var ts3 = Instant.now();

        var externalId1 = UUID.randomUUID();
        var externalId2 = UUID.randomUUID();
        var externalId3 = UUID.randomUUID();
        var externalId4 = UUID.randomUUID();

        var outboxEntity1 = new OutboxEntity(null, externalId1, OutboxEventType.HUB_CAPACITY_DEPLETED,
                "hub-test-" + seed, objectMapper.writeValueAsString(new HubCapacityDepletedEvent(UUID.randomUUID(), ts1, 100L + seed,
                TransportHubService.MIN_CAPACITY_ALARM, "hub-test-" + seed)),
                OutboxEventStatus.NEW, ts1, ts1, 0, null, null);
        var outboxEntity2 = new OutboxEntity(null, externalId2, OutboxEventType.HUB_CAPACITY_DEPLETED,
                "hub-test-" + seed, objectMapper.writeValueAsString(new HubCapacityDepletedEvent(UUID.randomUUID(), ts2,100L + seed,
                TransportHubService.MIN_CAPACITY_ALARM - 1, "hub-test-" + seed)),
                OutboxEventStatus.NEW, ts2, ts2, 0, null, null);
        var outboxEntity3 = new OutboxEntity(null, externalId3, OutboxEventType.HUB_CAPACITY_DEPLETED,
                "hub-test-" + seed, objectMapper.writeValueAsString(new HubCapacityDepletedEvent(UUID.randomUUID(), ts3,100L + seed,
                TransportHubService.MIN_CAPACITY_ALARM - 2, "hub-test-" + seed)),
                OutboxEventStatus.NEW, ts3, ts3, 0, null, null);
        seed++;
        var outboxEntity4 = new OutboxEntity(null, externalId4, OutboxEventType.HUB_CAPACITY_DEPLETED,
                "hub-test-" + seed, objectMapper.writeValueAsString(new HubCapacityDepletedEvent(UUID.randomUUID(), ts3,100L + seed,
                TransportHubService.MIN_CAPACITY_ALARM, "hub-test-" + seed)),
                OutboxEventStatus.NEW, ts3, ts3, 0, null, null);

        outboxRepository.insert(outboxEntity1);
        outboxRepository.insert(outboxEntity2);
        outboxRepository.insert(outboxEntity3);
        outboxRepository.insert(outboxEntity4);
        return List.of(outboxEntity1, outboxEntity2, outboxEntity3, outboxEntity4);
    }
}