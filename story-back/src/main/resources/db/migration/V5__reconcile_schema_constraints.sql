CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_banned ON users(banned);
CREATE INDEX IF NOT EXISTS idx_applications_user_id ON applications(user_id);
CREATE INDEX IF NOT EXISTS idx_applications_contest_id ON applications(contest_id);
CREATE INDEX IF NOT EXISTS idx_works_application_id ON works(application_id);
CREATE INDEX IF NOT EXISTS idx_members_application_id ON members(application_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_event_settings_singleton'
          AND conrelid = 'event_settings'::regclass
    ) THEN
        ALTER TABLE event_settings
            ADD CONSTRAINT chk_event_settings_singleton CHECK (id = 1);
    END IF;
END
$$;
