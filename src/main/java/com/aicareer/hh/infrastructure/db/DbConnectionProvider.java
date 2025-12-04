package com.aicareer.hh.infrastructure.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnectionProvider {

    private final String url;
    private final String user;
    private final String password;

    public DbConnectionProvider() {
        this.url = getOrDefault("DB_URL", "jdbc:postgresql://localhost:5433/aicareer");
        this.user = getOrDefault("DB_USER", "aicareer");
        this.password = getOrDefault("DB_PASSWORD", "aicareer");
    }

    private String getOrDefault(String key, String def) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? def : value;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}