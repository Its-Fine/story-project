CREATE TABLE IF NOT EXISTS roles (
    name VARCHAR(80) PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS permissions (
    name VARCHAR(120) PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_name VARCHAR(80) NOT NULL,
    PRIMARY KEY (user_id, role_name)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_name VARCHAR(80) NOT NULL,
    permission_name VARCHAR(120) NOT NULL,
    PRIMARY KEY (role_name, permission_name)
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
SELECT 'PARTICIPANT', name
FROM permissions
WHERE name IN (
    'contest:read',
    'event:read',
    'application:read:own',
    'application:write:own',
    'work:read:own',
    'work:write:own'
);

INSERT INTO role_permissions (role_name, permission_name)
SELECT 'ADMIN', name FROM permissions;
