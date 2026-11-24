CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_name VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_id, role_name)
);
