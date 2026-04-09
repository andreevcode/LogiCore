--liquibase formatted sql

--changeset andreevcode:003-create_logistics.outbox_with_types
CREATE TYPE logistics.outbox_event_status as ENUM ('NEW', 'PROCESSING', 'SENT', 'SKIPPED', 'FAILED');

CREATE TYPE logistics.outbox_event_type as ENUM ('HUB_CAPACITY_DEPLETED');

CREATE TABLE logistics.outbox (
       id BIGSERIAL PRIMARY KEY,
       external_id UUID NOT NULL UNIQUE,
       event_type logistics.outbox_event_type NOT NULL,
       aggregation_id TEXT NOT NULL,
       payload JSONB NOT NULL,
       event_status logistics.outbox_event_status NOT NULL DEFAULT 'NEW',
       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
       retry_count INTEGER NOT NULL DEFAULT 0,
       last_error TEXT,
       trace_id TEXT
);

CREATE INDEX idx_outbox_status_created ON logistics.outbox (event_status, created_at)
    WHERE event_status IN ('NEW', 'FAILED');

--rollback DROP TABLE logistics.outbox;
--rollback DROP TYPE logistics.outbox_event_status;
--rollback DROP TYPE logistics.outbox_event_type;
