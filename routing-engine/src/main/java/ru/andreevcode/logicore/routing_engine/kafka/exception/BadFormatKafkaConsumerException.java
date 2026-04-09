package ru.andreevcode.logicore.routing_engine.kafka.exception;

public class BadFormatKafkaConsumerException extends RuntimeException{

    public BadFormatKafkaConsumerException(String message) {
        super(message);
    }
}
