-- Add application review fields expected by ApplicationEntity.
-- Safe to run against both existing and newly initialized PostgreSQL databases.

ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'PENDING';

ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- Normalize any legacy NULL values before relying on the NOT NULL constraint.
UPDATE applications
SET status = 'PENDING'
WHERE status IS NULL;

ALTER TABLE applications
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ALTER COLUMN status SET NOT NULL;
