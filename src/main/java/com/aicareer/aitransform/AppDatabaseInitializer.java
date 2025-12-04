package com.aicareer.aitransform;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.hh.tools.VacancyResourceImporter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Applies the SQL schema and seed data shipped with the project
 * to the configured PostgreSQL database.
 */
public class AppDatabaseInitializer {

    private final DbConnectionProvider connectionProvider;

    public AppDatabaseInitializer(DbConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void applySchemaAndData() {
        executeSqlResource("db/schema.sql");
        executeSqlResource("db/data.sql");
        new VacancyResourceImporter(connectionProvider).importAllFromResources();
    }

    private void executeSqlResource(String resourcePath) {
        String sql = readResource(resourcePath);
        // allow multiple statements separated by ';'
        Arrays.stream(sql.split(";"))
                .map(String::trim)
                .filter(chunk -> !chunk.isEmpty())
                .forEach(this::executeStatement);
    }

    private void executeStatement(String statementSql) {
        try (Connection connection = connectionProvider.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(statementSql);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to execute SQL statement: " + statementSql, e);
        }
    }

    private String readResource(String resourcePath) {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read resource: " + resourcePath, e);
        }
    }
}
