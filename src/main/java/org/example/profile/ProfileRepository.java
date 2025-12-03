package org.example.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.db.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ProfileRepository {

    private final ObjectMapper mapper = new ObjectMapper();

    public void save(Profile profile) {
        String sql = """
            INSERT INTO user_profiles (user_id, target_role, skills, experience_years, updated_at)
            VALUES (?, ?, ?::jsonb, ?, ?)
            ON CONFLICT (user_id) DO UPDATE SET
                target_role = EXCLUDED.target_role,
                skills = EXCLUDED.skills,
                experience_years = EXCLUDED.experience_years,
                updated_at = EXCLUDED.updated_at
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, profile.getUserId());
            ps.setString(2, profile.getTargetRole());
            ps.setString(3, mapper.writeValueAsString(profile.getSkills()));
            ps.setInt(4, profile.getExperienceYears());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();

        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException("Не удалось сохранить профиль", e);
        }
    }

    public Optional<Profile> findByUserId(String userId) {
        String sql = "SELECT * FROM user_profiles WHERE user_id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Map<String, Integer> skills = readSkills(rs.getString("skills"));

                return Optional.of(new Profile(
                        rs.getString("user_id"),
                        rs.getString("target_role"),
                        skills,
                        rs.getInt("experience_years")
                ));
            }

            return Optional.empty();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке профиля", e);
        }
    }

    public List<Profile> findAll() {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT * FROM user_profiles";

        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Integer> skills = readSkills(rs.getString("skills"));

                profiles.add(new Profile(
                        rs.getString("user_id"),
                        rs.getString("target_role"),
                        skills,
                        rs.getInt("experience_years")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке всех профилей", e);
        }

        return profiles;
    }

    private Map<String, Integer> readSkills(String json) throws JsonProcessingException {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return mapper.readValue(
                json,
                mapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class)
        );
    }
}
