package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String URL = "jdbc:postgresql://localhost:5432/aicareer?sslmode=disable&applicationName=AI-Career-Client";
    private static final String USER = "aicareer";
    private static final String PASSWORD = "aicareer";

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

        } catch (SQLException e) {
            throw new RuntimeException("Init schema failed", e);
        }
    }
}