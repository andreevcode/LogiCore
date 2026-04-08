package ru.andreevcode.logicore.corelogistics;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test") // Spring look for application-test.properties
@Testcontainers
public abstract class BaseIT {
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Container
    @ServiceConnection
    protected static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16")
            .withInitScript("init-test-db.sql");

    @Container
    protected static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("TRUNCATE TABLE logistics.transport_hub, logistics.outbox RESTART IDENTITY CASCADE");
    }
}
