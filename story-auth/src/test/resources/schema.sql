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
    ('application:write:own', 'Create and edit own applications'),
    ('role:manage', 'Manage roles');

INSERT INTO role_permissions (role_name, permission_name) VALUES
    ('PARTICIPANT', 'application:write:own'),
    ('ADMIN', 'role:manage');
