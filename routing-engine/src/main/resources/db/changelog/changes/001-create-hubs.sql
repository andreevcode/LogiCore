--liquibase formatted sql

--changeset andreevcode:001-create_hubs
CREATE TABLE hubs (
       id BIGINT PRIMARY KEY,
       code text NOT NULL,
       capacity int NOT NULL,
       capacity_updated_at timestamp with time zone
);

--rollback DROP TABLE IF EXISTS hubs;
