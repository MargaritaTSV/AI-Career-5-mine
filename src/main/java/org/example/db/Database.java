package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String URL = System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:postgresql://localhost:5433/aicareer"
    );
    private static final String USER = "aicareer";
    private static final String PASSWORD = "aicareer";

    public static Connection get() {
        try {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException ignored) {
            }

            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            String help = "PostgreSQL connection failed: " + msg + "\n\n"
                    + "This application requires the role/user 'aicareer' and connects with that identity.\n"
                    + "Checked connection settings:\n"
                    + "  DB_URL=" + URL + "\n"
                    + "  DB_USER=" + USER + "\n\n"
                    + "If you see 'role \"aicareer\" does not exist' — create the role and the database, for example:\n"
                    + "  sudo -u postgres createuser -P aicareer\n"
                    + "  sudo -u postgres createdb -O aicareer aicareer\n\n"
                    + "Or configure your Postgres to have a role 'aicareer' and password 'aicareer'.\n";
            throw new RuntimeException(help, e);
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
                  salary_from   INT,
                  salary_to     INT,
                  currency      TEXT,
                  description   TEXT,
                  url           TEXT,
                  source        TEXT,
                  published_at  TEXT,
                  score         INT
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Init schema failed", e);
        }
    }
}