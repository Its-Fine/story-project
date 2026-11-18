REVOKE ALL ON ALL TABLES IN SCHEMA public FROM story_admin;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM story_admin;

GRANT SELECT, UPDATE ON TABLE users TO story_admin;

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
    contests,
    applications,
    works,
    members,
    event_settings
TO story_admin;

GRANT USAGE, SELECT ON SEQUENCE
    contests_id_seq,
    applications_id_seq,
    works_id_seq,
    members_id_seq
TO story_admin;
