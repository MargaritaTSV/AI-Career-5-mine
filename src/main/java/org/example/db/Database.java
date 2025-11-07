package org.example.db;

import java.sql.*;

public class Database {
    private static final String URL  = "jdbc:h2:./data/aicareer;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASS = "";

    public static Connection get() throws SQLException {
        try {
            Class.forName("org.h2.Driver"); // ✅ важно! Без этого драйвер не подхватывается в IntelliJ
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 driver not found", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void init() {
        String usersSql = """
            CREATE TABLE IF NOT EXISTS users (
              id            VARCHAR(64)  PRIMARY KEY,
              email         VARCHAR(320) UNIQUE NOT NULL,
              password_hash VARCHAR(256) NOT NULL,
              name          VARCHAR(200),
              created_at    TIMESTAMP     NOT NULL
            );
            """;

        String profilesSql = """
            CREATE TABLE IF NOT EXISTS profiles (
              user_id          VARCHAR(64)  PRIMARY KEY,
              target_role      VARCHAR(200),
              experience_years INT,
              skills           TEXT,      -- для MVP: csv "java,spring,git"
              education        VARCHAR(300),
              english_level    VARCHAR(20),
              motivation       TEXT,
              desired_salary   DOUBLE,
              updated_at       TIMESTAMP,
              CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );
            """;

        try (Connection c = get(); Statement st = c.createStatement()) {
            st.execute(usersSql);
            st.execute(profilesSql);
        } catch (SQLException e) {
            throw new RuntimeException("Init schema failed", e);
        }
    }

    public static void smokeTest() {
        try (Connection c = get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) {
            if (rs.next()) System.out.println("H2 OK, SELECT 1 -> " + rs.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Smoke test failed", e);
        }
    }
}
