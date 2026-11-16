-- Persist the global application submission state across backend restarts.
-- The table contains exactly one settings row with id = 1.

CREATE TABLE IF NOT EXISTS event_settings (
    id SMALLINT PRIMARY KEY,
    applications_closed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_event_settings_singleton CHECK (id = 1)
);

INSERT INTO event_settings (id, applications_closed)
VALUES (1, FALSE)
ON CONFLICT (id) DO NOTHING;
