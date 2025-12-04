
CREATE TABLE IF NOT EXISTS app_users (
    id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(320) UNIQUE NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    name VARCHAR(200),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS app_profiles (
    user_id VARCHAR(64) PRIMARY KEY,
    target_role VARCHAR(200),
    experience_years INT,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS app_skills (
    user_id VARCHAR(64) NOT NULL,
    skill_name VARCHAR(100) NOT NULL,
    level INT NOT NULL,
    PRIMARY KEY (user_id, skill_name),
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

-- Вакансии, которые сохраняем после парсинга HH
CREATE TABLE IF NOT EXISTS vacancy (
    id TEXT PRIMARY KEY,
    title TEXT,
    company TEXT,
    city TEXT,
    experience TEXT,
    employment TEXT,
    schedule TEXT,
    salary_from INT,
    salary_to INT,
    currency TEXT,
    description TEXT,
    url TEXT,
    source TEXT,
    published_at TEXT,
    score INT
);
