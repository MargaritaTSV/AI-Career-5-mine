package com.aicareer.hh.repository;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.hh.model.Vacancy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class JdbcVacancyRepository implements VacancyRepository {

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
                    score
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
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
                    score        = EXCLUDED.score
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
                stmt.setInt(i, v.getScore());

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
    public List<Vacancy> findBySource(String source) {
        String sql = """
                SELECT id, title, company, city, experience, employment, schedule,
                       salary_from, salary_to, currency, description, url, source, published_at, score
                FROM vacancy
                WHERE source = ?
                ORDER BY score DESC NULLS LAST, published_at DESC
                """;

        List<Vacancy> vacancies = new LinkedList<>();
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, source);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Vacancy v = new Vacancy();
                    v.setId(rs.getString("id"));
                    v.setTitle(rs.getString("title"));
                    v.setCompany(rs.getString("company"));
                    v.setCity(rs.getString("city"));
                    v.setExperience(rs.getString("experience"));
                    v.setEmployment(rs.getString("employment"));
                    v.setSchedule(rs.getString("schedule"));
                    v.setSalaryFrom((Integer) rs.getObject("salary_from"));
                    v.setSalaryTo((Integer) rs.getObject("salary_to"));
                    v.setCurrency(rs.getString("currency"));
                    v.setDescription(rs.getString("description"));
                    v.setUrl(rs.getString("url"));
                    v.setSource(rs.getString("source"));
                    v.setPublishedAt(rs.getString("published_at"));
                    v.setScore(rs.getInt("score"));
                    vacancies.add(v);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при загрузке вакансий из БД", e);
        }
        return vacancies;
    }
}