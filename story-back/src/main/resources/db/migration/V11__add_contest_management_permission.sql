INSERT INTO permissions (name, description)
VALUES ('contest:manage', 'Create, update and delete contest nominations')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_name, permission_name)
VALUES ('ADMIN', 'contest:manage')
ON CONFLICT DO NOTHING;
