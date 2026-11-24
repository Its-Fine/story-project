ALTER TABLE works ALTER COLUMN file_name TYPE VARCHAR(1024);

CREATE TABLE storage_cleanup_tasks (
    id BIGSERIAL PRIMARY KEY,
    object_name VARCHAR(1024) NOT NULL UNIQUE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_storage_cleanup_tasks_due
    ON storage_cleanup_tasks (next_attempt_at, created_at);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE storage_cleanup_tasks TO story_back;
GRANT USAGE, SELECT ON SEQUENCE storage_cleanup_tasks_id_seq TO story_back;
