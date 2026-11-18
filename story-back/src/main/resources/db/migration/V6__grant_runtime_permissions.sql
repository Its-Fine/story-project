REVOKE ALL ON ALL TABLES IN SCHEMA public FROM story_auth, story_back;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM story_auth, story_back;

GRANT SELECT, INSERT, UPDATE ON TABLE users TO story_auth;
GRANT USAGE, SELECT ON SEQUENCE users_id_seq TO story_auth;

GRANT SELECT, INSERT, UPDATE ON TABLE users TO story_back;
GRANT USAGE, SELECT ON SEQUENCE users_id_seq TO story_back;

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
    contests,
    applications,
    works,
    members,
    event_settings
TO story_back;

GRANT USAGE, SELECT ON SEQUENCE
    contests_id_seq,
    applications_id_seq,
    works_id_seq,
    members_id_seq
TO story_back;
