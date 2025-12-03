package org.example.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class TestDataSeeder {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private TestDataSeeder() {
    }

    public static String seedAndGetDefaultUser() {
        List<UserProfileSeed> seeds = List.of(
                new UserProfileSeed(
                        "11111111-1111-1111-1111-111111111111",
                        "java@example.com",
                        "2ca0033",
                        "Мария Бэкенд",
                        "Java Backend Developer",
                        Map.of("java", 1, "spring", 1, "sql", 1, "docker", 1, "kafka", 0, "aws", 0),
                        2
                ),
                new UserProfileSeed(
                        "22222222-2222-2222-2222-222222222222",
                        "datasci@example.com",
                        "4889ba9b",
                        "Илья Дата",
                        "Data Scientist",
                        Map.of("python", 1, "pandas", 1, "numpy", 1, "machine_learning", 1, "sql", 1, "spark", 0, "aws", 0),
                        3
                ),
                new UserProfileSeed(
                        "33333333-3333-3333-3333-333333333333",
                        "frontend@example.com",
                        "4103158",
                        "Анна Фронтенд",
                        "Frontend JavaScript Developer",
                        Map.of("javascript", 1, "react", 1, "typescript", 1, "html", 1, "css", 1, "docker", 0),
                        4
                )
        );

        seeds.forEach(TestDataSeeder::insertUserWithProfile);
        return seeds.get(0).id();
    }

    private static void insertUserWithProfile(UserProfileSeed seed) {
        String userSql = """
                INSERT INTO users (id, email, password_hash, name, created_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """;

        String profileSql = """
                INSERT INTO user_profiles (user_id, target_role, experience_years, skills, updated_at)
                VALUES (?, ?, ?, ?::jsonb, ?)
                ON CONFLICT (user_id) DO UPDATE SET
                    target_role = EXCLUDED.target_role,
                    experience_years = EXCLUDED.experience_years,
                    skills = EXCLUDED.skills,
                    updated_at = EXCLUDED.updated_at
                """;

        try (Connection connection = Database.get();
             PreparedStatement userPs = connection.prepareStatement(userSql);
             PreparedStatement profilePs = connection.prepareStatement(profileSql)) {

            userPs.setString(1, seed.id());
            userPs.setString(2, seed.email());
            userPs.setString(3, seed.passwordHash());
            userPs.setString(4, seed.name());
            userPs.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            userPs.executeUpdate();

            profilePs.setString(1, seed.id());
            profilePs.setString(2, seed.targetRole());
            profilePs.setInt(3, seed.experienceYears());
            profilePs.setString(4, toJson(seed.skills()));
            profilePs.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            profilePs.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось добавить тестовые данные", e);
        }
    }

    private static String toJson(Map<String, Integer> skills) {
        try {
            return MAPPER.writeValueAsString(skills);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать навыки в JSON", e);
        }
    }

    private record UserProfileSeed(
            String id,
            String email,
            String passwordHash,
            String name,
            String targetRole,
            Map<String, Integer> skills,
            int experienceYears
    ) {
    }
}
