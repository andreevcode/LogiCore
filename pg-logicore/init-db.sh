#!/bin/bash
set -e

# Подхватываем пароль из переменной, которую прокинул Docker
export PGPASSWORD="$POSTGRES_PASSWORD"

# Подключаемся к базе под суперпользователем и выполняем блок
psql -v ON_ERROR_STOP=1 --host pg-logicore --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
DO \$$
BEGIN
   -- Идемпотентное создание схем
   CREATE SCHEMA IF NOT EXISTS routing;
   CREATE SCHEMA IF NOT EXISTS logistics;

    -- Логика для первого сервиса
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$LOGISTICS_USER') THEN
        CREATE USER $LOGISTICS_USER WITH PASSWORD '$LOGISTICS_PASS';
        
        -- Раздаем права ТОЛЬКО при создании
        GRANT ALL PRIVILEGES ON SCHEMA logistics TO $LOGISTICS_USER;
        ALTER ROLE $LOGISTICS_USER SET search_path TO logistics;
        
        RAISE NOTICE 'User logistics_user created and configured.';
    END IF;

    -- Логика для второго сервиса (routing)
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$ROUTING_USER') THEN
        CREATE USER $ROUTING_USER WITH PASSWORD '$ROUTING_PASS';
        
        GRANT ALL PRIVILEGES ON SCHEMA routing TO $ROUTING_USER;
        ALTER ROLE $ROUTING_USER SET search_path TO routing;
        
        RAISE NOTICE 'User routing_user created and configured.';
    END IF;
END
\$$;
EOSQL