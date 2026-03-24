package ru.andreevcode.logicore.corelogistics;

import org.springframework.boot.SpringApplication;

public class TestCoreLogisticsApplication {

    public static void main(String[] args) {
        SpringApplication.from(CoreLogisticsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
