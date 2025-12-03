package com.aicareer.aitransform;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads user info from the database and saves it as JSON for the AI pipeline.
 */
public class UserInfoExporter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final DbConnectionProvider connectionProvider;

    public UserInfoExporter(DbConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Optional<ProfileSnapshot> findByEmail(String email) {
        String sql = """
                SELECT u.id, u.email, u.name, u.created_at, p.target_role, p.experience_years
                FROM app_users u
                LEFT JOIN app_profiles p ON p.user_id = u.id
                WHERE u.email = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                String userId = rs.getString("id");
                Map<String, Integer> skills = loadSkills(userId);
                return Optional.of(new ProfileSnapshot(
                        userId,
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("target_role"),
                        rs.getInt("experience_years"),
                        asOffset(rs.getObject("created_at")),
                        skills
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load user from DB", e);
        }
    }

    public Path writeUserSkillMatrix(Map<String, Integer> skills, Path outputPath) {
        try {
            Files.createDirectories(outputPath.toAbsolutePath().getParent());
            MAPPER.writeValue(outputPath.toFile(), skills);
            return outputPath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write user skill matrix", e);
        }
    }

    private Map<String, Integer> loadSkills(String userId) throws SQLException {
        String sql = "SELECT skill_name, level FROM app_skills WHERE user_id = ?";
        Map<String, Integer> skills = new LinkedHashMap<>();

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    skills.put(rs.getString("skill_name"), rs.getInt("level"));
                }
            }
        }
        return skills;
    }

    private OffsetDateTime asOffset(Object createdAt) {
        if (createdAt instanceof OffsetDateTime offset) {
            return offset;
        }
        if (createdAt instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atOffset(OffsetDateTime.now().getOffset());
        }
        return null;
    }

    public record ProfileSnapshot(
            String id,
            String email,
            String name,
            String targetRole,
            Integer experienceYears,
            OffsetDateTime createdAt,
            Map<String, Integer> skills
    ) {
    }
}
