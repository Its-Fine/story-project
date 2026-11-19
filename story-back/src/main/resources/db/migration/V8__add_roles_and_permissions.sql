CREATE TABLE roles (
    name VARCHAR(80) PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE permissions (
    name VARCHAR(120) PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(80) NOT NULL REFERENCES roles(name) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_name)
);

CREATE TABLE role_permissions (
    role_name VARCHAR(80) NOT NULL REFERENCES roles(name) ON DELETE CASCADE,
    permission_name VARCHAR(120) NOT NULL REFERENCES permissions(name) ON DELETE CASCADE,
    PRIMARY KEY (role_name, permission_name)
);

CREATE TABLE role_change_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    target_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    previous_roles TEXT NOT NULL,
    new_roles TEXT NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO roles (name, description) VALUES
    ('PARTICIPANT', 'Contest participant'),
    ('ADMIN', 'Full application administrator');

INSERT INTO permissions (name, description) VALUES
    ('contest:read', 'Read contest catalogue'),
    ('event:read', 'Read event admission state'),
    ('application:read:own', 'Read own applications'),
    ('application:write:own', 'Create, update and delete own applications'),
    ('work:read:own', 'Read own works and files'),
    ('work:write:own', 'Create and delete own works'),
    ('user:manage', 'Read and update user administration data'),
    ('role:manage', 'Assign application roles to users'),
    ('application:read:any', 'Read any application'),
    ('application:moderate', 'Change application moderation status'),
    ('work:read:any', 'Read any work and file'),
    ('work:rate', 'Set and clear work ratings'),
    ('event:manage', 'Open and close application admission'),
    ('report:export', 'Export administrative reports');

INSERT INTO role_permissions (role_name, permission_name)
SELECT 'PARTICIPANT', name FROM permissions
WHERE name IN (
    'contest:read', 'event:read',
    'application:read:own', 'application:write:own',
    'work:read:own', 'work:write:own'
);

INSERT INTO role_permissions (role_name, permission_name)
SELECT 'ADMIN', name FROM permissions;

INSERT INTO user_roles (user_id, role_name)
SELECT id, role FROM users
WHERE role IN ('PARTICIPANT', 'ADMIN')
ON CONFLICT DO NOTHING;

GRANT SELECT ON TABLE roles, permissions, role_permissions TO story_auth, story_back, story_admin;
GRANT SELECT, INSERT, DELETE ON TABLE user_roles TO story_auth;
GRANT SELECT, INSERT, DELETE ON TABLE user_roles TO story_back;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE user_roles, role_permissions TO story_admin;
GRANT SELECT, INSERT ON TABLE role_change_audit TO story_admin;
GRANT USAGE, SELECT ON SEQUENCE role_change_audit_id_seq TO story_admin;
