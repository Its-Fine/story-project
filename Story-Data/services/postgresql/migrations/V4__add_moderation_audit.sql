-- Add moderation audit and optimistic locking without changing existing data.

ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE works
    ADD COLUMN IF NOT EXISTS rated_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS rated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
