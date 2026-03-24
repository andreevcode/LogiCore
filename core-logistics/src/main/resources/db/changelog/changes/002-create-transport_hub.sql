--liquibase formatted sql

--changeset andreevcode:002-create_logistics.transport_hub
CREATE TABLE logistics.transport_hub (
       id BIGSERIAL PRIMARY KEY,
       name text NOT NULL,
       capacity int NOT NULL,
       code text NOT NULL
);

--rollback DROP TABLE logistics.transport_hub;
