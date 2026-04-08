--liquibase formatted sql

--changeset andreevcode:001-create_hubs
CREATE TABLE hubs (
       id BIGSERIAL PRIMARY KEY,
       name text NOT NULL,
       code text NOT NULL,
       capacity int NOT NULL,
       capacity_last_updated_at timestamp with time zone
);

--rollback DROP TABLE IF EXISTS hubs;
