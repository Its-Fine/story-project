GRANT INSERT, UPDATE, DELETE ON TABLE roles TO story_admin;

CREATE TABLE role_definition_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role_name VARCHAR(80) NOT NULL,
    action VARCHAR(20) NOT NULL,
    previous_permissions TEXT NOT NULL,
    new_permissions TEXT NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

GRANT SELECT, INSERT ON TABLE role_definition_audit TO story_admin;
GRANT USAGE, SELECT ON SEQUENCE role_definition_audit_id_seq TO story_admin;
