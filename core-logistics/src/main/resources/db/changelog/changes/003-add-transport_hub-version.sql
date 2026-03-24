--liquibase formatted sql

--changeset andreevcode:003-add-transport_hub-version
ALTER TABLE logistics.transport_hub ADD COLUMN version BIGINT DEFAULT 0;

--rollback ALTER TABLE logistics.transport_hub DROP COLUMN version;
