package ru.andreevcode.logicore.corelogistics.outbox;

public enum OutboxEventStatus {
    NEW("New message, ready to process"),
    PROCESSING("The sending process is running"),
    SENT("Message is successfully sent to broker"),
    SKIPPED("Message is obsolete and skipped"),
    FAILED("Error is found during last run");

    private final String description;

    OutboxEventStatus(String description) {
        this.description = description;
    }
}
