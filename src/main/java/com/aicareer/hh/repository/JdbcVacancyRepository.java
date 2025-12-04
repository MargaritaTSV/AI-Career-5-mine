package com.aicareer.hh.repository;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.hh.model.Vacancy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class JdbcVacancyRepository implements VacancyRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DbConnectionProvider connectionProvider;

    public JdbcVacancyRepository(DbConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void saveAll(Collection<Vacancy> vacancies) {
        if (vacancies == null || vacancies.isEmpty()) {
            return;
        }

        // ОДНА таблица vacancy, без skills
        // Ожидаем схему типа:
        // id (PK, text/varchar),
        // title, company, city, experience, employment, schedule,
        // salary_from, salary_to, currency,
        // description, url, source, published_at, score
        String sql = """
                INSERT INTO vacancy (
                    id,
                    title,
                    company,
                    city,
                    experience,
                    employment,
                    schedule,
                    salary_from,
                    salary_to,
                    currency,
                    description,
                    url,
                    source,
                    published_at,
                    score,
                    skills
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb
                )
                ON CONFLICT (id) DO UPDATE SET
                    title        = EXCLUDED.title,
                    company      = EXCLUDED.company,
                    city         = EXCLUDED.city,
                    experience   = EXCLUDED.experience,
                    employment   = EXCLUDED.employment,
                    schedule     = EXCLUDED.schedule,
                    salary_from  = EXCLUDED.salary_from,
                    salary_to    = EXCLUDED.salary_to,
                    currency     = EXCLUDED.currency,
                    description  = EXCLUDED.description,
                    url          = EXCLUDED.url,
                    source       = EXCLUDED.source,
                    published_at = EXCLUDED.published_at,
                    score        = EXCLUDED.score,
                    skills       = EXCLUDED.skills
                """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (Vacancy v : vacancies) {
                if (v == null || v.getId() == null) {
                    continue;
                }

                int i = 1;
                stmt.setString(i++, v.getId());
                stmt.setString(i++, v.getTitle());
                stmt.setString(i++, v.getCompany());
                stmt.setString(i++, v.getCity());
                stmt.setString(i++, v.getExperience());
                stmt.setString(i++, v.getEmployment());
                stmt.setString(i++, v.getSchedule());

                if (v.getSalaryFrom() != null) {
                    stmt.setInt(i++, v.getSalaryFrom());
                } else {
                    stmt.setNull(i++, Types.INTEGER);
                }

                if (v.getSalaryTo() != null) {
                    stmt.setInt(i++, v.getSalaryTo());
                } else {
                    stmt.setNull(i++, Types.INTEGER);
                }

                stmt.setString(i++, v.getCurrency());
                stmt.setString(i++, v.getDescription());
                stmt.setString(i++, v.getUrl());
                stmt.setString(i++, v.getSource());
                stmt.setString(i++, v.getPublishedAt());
                stmt.setInt(i++, v.getScore());
                stmt.setString(i, toJson(v.getSkills()));

                stmt.addBatch();
            }

            stmt.executeBatch();
            conn.commit();
            System.out.println("✅ Вакансии сохранены в БД: " + vacancies.size());
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении вакансий в БД", e);
        }
    }

    @Override
    public List<Vacancy> findByRole(String role, int limit) {
        String like = "%" + role.toLowerCase(Locale.ROOT) + "%";
        String sql = """
                SELECT id, title, company, city, experience, employment, schedule, salary_from, salary_to,
                       currency, description, url, source, published_at, score, skills
                FROM vacancy
                WHERE (LOWER(title) LIKE ? OR LOWER(description) LIKE ?)
                ORDER BY score DESC NULLS LAST, published_at DESC NULLS LAST
                LIMIT ?
                """;

        List<Vacancy> result = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Vacancy vacancy = new Vacancy();
                    vacancy.setId(rs.getString("id"));
                    vacancy.setTitle(rs.getString("title"));
                    vacancy.setCompany(rs.getString("company"));
                    vacancy.setCity(rs.getString("city"));
                    vacancy.setExperience(rs.getString("experience"));
                    vacancy.setEmployment(rs.getString("employment"));
                    vacancy.setSchedule(rs.getString("schedule"));
                    int salaryFrom = rs.getInt("salary_from");
                    if (!rs.wasNull()) {
                        vacancy.setSalaryFrom(salaryFrom);
                    }
                    int salaryTo = rs.getInt("salary_to");
                    if (!rs.wasNull()) {
                        vacancy.setSalaryTo(salaryTo);
                    }
                    vacancy.setCurrency(rs.getString("currency"));
                    vacancy.setDescription(rs.getString("description"));
                    vacancy.setUrl(rs.getString("url"));
                    vacancy.setSource(rs.getString("source"));
                    vacancy.setPublishedAt(rs.getString("published_at"));
                    vacancy.setScore(rs.getInt("score"));
                    String skillsJson = rs.getString("skills");
                    if (skillsJson != null && !skillsJson.isBlank()) {
                        vacancy.setSkills(MAPPER.readValue(skillsJson, new TypeReference<>() {
                        }));
                    }
                    result.add(vacancy);
                }
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load vacancies for role: " + role, e);
        }
    }

    private String toJson(List<String> skills) {
        try {
            return skills == null ? "[]" : MAPPER.writeValueAsString(skills);
        } catch (Exception e) {
            return "[]";
        }
    }
}