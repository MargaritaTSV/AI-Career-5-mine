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
            MERGE INTO profiles (user_id, target_role, skills, experience_years, updated_at)
            KEY (user_id)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, profile.getUserId());
            ps.setString(2, profile.getTargetRole());
            ps.setString(3, mapper.writeValueAsString(profile.getSkills())); // JSON skills
            ps.setInt(4, profile.getExperienceYears());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException("Не удалось сохранить профиль", e);
        }
    }


    public Optional<Profile> findByUserId(String userId) {
        String sql = "SELECT * FROM profiles WHERE user_id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String skillsJson = rs.getString("skills");
                Map<String, Integer> skills = mapper.readValue(
                        skillsJson, mapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class)
                );

                return Optional.of(new Profile(
                        rs.getString("user_id"),
                        rs.getString("target_role"),
                        skills,
                        rs.getInt("experience_years")
                ));
            }
            return Optional.empty();

        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException("Ошибка при загрузке профиля", e);
        }
    }


    public List<Profile> findAll() {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT * FROM profiles";
        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String skillsJson = rs.getString("skills");
                Map<String, Integer> skills = mapper.readValue(
                        skillsJson, mapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class)
                );

                profiles.add(new Profile(
                        rs.getString("user_id"),
                        rs.getString("target_role"),
                        skills,
                        rs.getInt("experience_years")
                ));
            }
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException("Ошибка при загрузке всех профилей", e);
        }
        return profiles;
    }
}
