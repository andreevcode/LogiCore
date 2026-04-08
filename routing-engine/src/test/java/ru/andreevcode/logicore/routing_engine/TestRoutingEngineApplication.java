package ru.andreevcode.logicore.routing_engine;

import org.springframework.boot.SpringApplication;

public class TestRoutingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.from(RoutingEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
