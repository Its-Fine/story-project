* text=auto eol=lf
*.bat text eol=crlf
*.cmd text eol=crlf
*.png binary
*.jpg binary
*.jar binary-- init.sql
-- Создание таблиц для JPA сущностей

-- Таблица users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    closed_access BOOLEAN NOT NULL DEFAULT FALSE,
    banned BOOLEAN NOT NULL DEFAULT FALSE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    consent BOOLEAN NOT NULL,
    parent_consent BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для users
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_banned ON users(banned);

-- Таблица contests
CREATE TABLE IF NOT EXISTS contests (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    start_age INTEGER NOT NULL,
    end_age INTEGER NOT NULL
);

-- Таблица applications
CREATE TABLE IF NOT EXISTS applications (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    city TEXT NOT NULL,
    organization TEXT,
    mentor VARCHAR(255),
    is_team BOOLEAN NOT NULL DEFAULT FALSE,
    is_curator BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    contest_id BIGINT NOT NULL,
    CONSTRAINT fk_applications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_applications_contest FOREIGN KEY (contest_id) REFERENCES contests(id) ON DELETE RESTRICT
);

-- Индексы для applications
CREATE INDEX IF NOT EXISTS idx_applications_user_id ON applications(user_id);
CREATE INDEX IF NOT EXISTS idx_applications_contest_id ON applications(contest_id);

-- Таблица works
CREATE TABLE IF NOT EXISTS works (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    file_name VARCHAR(400) NOT NULL,
    rate INTEGER,
    is_rated BOOLEAN NOT NULL DEFAULT FALSE,
    file_size INTEGER NOT NULL,
    broken BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    application_id BIGINT NOT NULL,
    CONSTRAINT fk_works_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT uk_works_name_application UNIQUE (name, application_id)
);

-- Индексы для works
CREATE INDEX IF NOT EXISTS idx_works_application_id ON works(application_id);

-- Таблица members
CREATE TABLE IF NOT EXISTS members (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    application_id BIGINT NOT NULL,
    CONSTRAINT fk_members_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);

-- Индексы для members
CREATE INDEX IF NOT EXISTS fk_members_application ON members(application_id);
