package com.aicareer.hh.repository;

import com.aicareer.hh.model.Vacancy;
import org.example.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

public class JdbcVacancyRepository implements VacancyRepository {

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

        Database.init();

        try (Connection conn = Database.get();
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
}