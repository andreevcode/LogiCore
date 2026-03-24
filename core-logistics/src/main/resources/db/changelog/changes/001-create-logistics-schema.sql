--liquibase formatted sql

--changeset andreevcode:001-create-schema-logistics
CREATE SCHEMA IF NOT EXISTS logistics;

--rollback DROP SCHEMA IF EXISTS logistics CASCADE;