CREATE TABLE user_security_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    target_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    setting_name VARCHAR(40) NOT NULL,
    previous_value BOOLEAN NOT NULL,
    new_value BOOLEAN NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

GRANT SELECT, INSERT ON TABLE user_security_audit TO story_admin;
GRANT USAGE, SELECT ON SEQUENCE user_security_audit_id_seq TO story_admin;
