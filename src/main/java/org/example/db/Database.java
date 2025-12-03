package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String URL = envOrDefault(
            "DB_URL",
            "jdbc:postgresql://localhost:5432/aicareer"
    );
    private static final String USER = envOrDefault("DB_USER", "aicareer");
    private static final String PASSWORD = envOrDefault("DB_PASSWORD", "aicareer");

    private static String envOrDefault(String key, String def) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? def : value;
    }

    public static Connection get() {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("PostgreSQL connection failed", e);
        }
    }

    public static void init() {
        try (Connection c = get(); Statement st = c.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                  id            VARCHAR(64) PRIMARY KEY,
                  email         VARCHAR(320) UNIQUE NOT NULL,
                  password_hash VARCHAR(256) NOT NULL,
                  name          VARCHAR(200),
                  created_at    TIMESTAMP NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS user_profiles (
                  user_id          VARCHAR(64) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                  target_role      VARCHAR(200),
                  experience_years INT,
                  skills           JSONB,
                  updated_at       TIMESTAMP
                )
            """);

            st.execute("""
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
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Init schema failed", e);
        }
    }
}