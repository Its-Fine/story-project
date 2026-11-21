-- Test-only schema. Hibernate creates the users table; Flyway V8 is PostgreSQL-only.
CREATE TABLE roles (
    name VARCHAR(80) PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(80) NOT NULL REFERENCES roles(name),
    PRIMARY KEY (user_id, role_name)
);

INSERT INTO roles (name, description) VALUES
    ('PARTICIPANT', 'Contest participant'),
    ('ADMIN', 'Application administrator');
