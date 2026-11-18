#!/bin/sh
set -eu

: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${STORY_MIGRATION_DB_PASSWORD:?STORY_MIGRATION_DB_PASSWORD is required}"
: "${STORY_AUTH_DB_PASSWORD:?STORY_AUTH_DB_PASSWORD is required}"
: "${STORY_BACK_DB_PASSWORD:?STORY_BACK_DB_PASSWORD is required}"

psql --set ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=database_name="$POSTGRES_DB" \
  --set=migration_password="$STORY_MIGRATION_DB_PASSWORD" \
  --set=auth_password="$STORY_AUTH_DB_PASSWORD" \
  --set=back_password="$STORY_BACK_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE story_migrator LOGIN PASSWORD %L', :'migration_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'story_migrator')
\gexec
SELECT format('ALTER ROLE story_migrator WITH LOGIN PASSWORD %L', :'migration_password')
\gexec

SELECT format('CREATE ROLE story_auth LOGIN PASSWORD %L', :'auth_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'story_auth')
\gexec
SELECT format('ALTER ROLE story_auth WITH LOGIN PASSWORD %L', :'auth_password')
\gexec

SELECT format('CREATE ROLE story_back LOGIN PASSWORD %L', :'back_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'story_back')
\gexec
SELECT format('ALTER ROLE story_back WITH LOGIN PASSWORD %L', :'back_password')
\gexec

GRANT CONNECT ON DATABASE :"database_name" TO story_migrator, story_auth, story_back;
GRANT USAGE, CREATE ON SCHEMA public TO story_migrator;
GRANT USAGE ON SCHEMA public TO story_auth, story_back;

-- Existing installations were created by POSTGRES_USER/Hibernate. Transfer only
-- application objects in public; database ownership and the admin role stay intact.
DO $$
DECLARE
    item RECORD;
BEGIN
    FOR item IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname = 'public' AND tableowner = current_user
    LOOP
        EXECUTE format('ALTER TABLE %I.%I OWNER TO story_migrator', item.schemaname, item.tablename);
    END LOOP;

    FOR item IN
        SELECT sequence_schema, sequence_name
        FROM information_schema.sequences
        WHERE sequence_schema = 'public'
    LOOP
        EXECUTE format('ALTER SEQUENCE %I.%I OWNER TO story_migrator', item.sequence_schema, item.sequence_name);
    END LOOP;
END
$$;
SQL
