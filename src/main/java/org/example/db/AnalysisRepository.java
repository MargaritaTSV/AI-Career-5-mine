package org.example.db;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.hh.model.Vacancy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AnalysisRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DbConnectionProvider connectionProvider;

    public AnalysisRepository(DbConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public UUID saveRun(String userId,
                        String targetRole,
                        List<Vacancy> vacancies,
                        Map<String, Integer> userMatrix,
                        Map<String, Integer> roleMatrix,
                        Map<String, String> statuses,
                        Map<String, List<String>> summary) {
        UUID runId = UUID.randomUUID();
        String sql = """
                INSERT INTO app_analysis_runs (
                    id, user_id, target_role, vacancies, user_matrix, role_matrix, statuses, summary, created_at
                ) VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, NOW())
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setObject(1, runId, Types.OTHER);
            ps.setString(2, userId);
            ps.setString(3, targetRole);
            ps.setString(4, toJson(vacancies));
            ps.setString(5, toJson(userMatrix));
            ps.setString(6, toJson(roleMatrix));
            ps.setString(7, toJson(statuses));
            ps.setString(8, toJson(summary));
            ps.executeUpdate();
            return runId;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to store analysis run", e);
        }
    }

    public void updateRoadmap(UUID runId, String roadmap) {
        String sql = "UPDATE app_analysis_runs SET roadmap = ? WHERE id = ?";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roadmap);
            ps.setObject(2, runId, Types.OTHER);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save roadmap", e);
        }
    }

    private String toJson(Object payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize payload", e);
        }
    }
}
