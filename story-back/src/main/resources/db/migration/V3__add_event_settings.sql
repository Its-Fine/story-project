CREATE TABLE IF NOT EXISTS event_settings (
    id SMALLINT PRIMARY KEY,
    applications_closed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_event_settings_singleton CHECK (id = 1)
);

-- Hibernate-created legacy tables can have the same NOT NULL columns without
-- database defaults. Normalize them before inserting the singleton row.
ALTER TABLE event_settings
    ALTER COLUMN applications_closed SET DEFAULT FALSE,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

INSERT INTO event_settings (id, applications_closed, updated_at)
VALUES (1, FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
