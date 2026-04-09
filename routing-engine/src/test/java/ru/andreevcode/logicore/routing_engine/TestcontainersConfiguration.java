package ru.andreevcode.logicore.routing_engine;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16"))
                .withInitScript("init-test-db.sql");
    }

    @Bean
    @ServiceConnection
    public KafkaContainer kafka() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));
    }

    @Bean
    public DynamicPropertyRegistrar dynamicPropertyRegistrar(
            KafkaContainer kafka) {
        return registry -> {
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        };
    }
}
