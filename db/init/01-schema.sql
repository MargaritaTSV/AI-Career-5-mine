CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(320) UNIQUE NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    name VARCHAR(200),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_profiles (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    target_role VARCHAR(200),
    experience_years INT,
    skills JSONB,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vacancy (
    id            TEXT PRIMARY KEY,
    title         TEXT,
    company       TEXT,
    city          TEXT,
    experience    TEXT,
    employment    TEXT,
    schedule      TEXT,
    salary_from   INTEGER,
    salary_to     INTEGER,
    currency      TEXT,
    description   TEXT,
    url           TEXT,
    source        TEXT,
    published_at  TEXT,
    score         INTEGER
);
