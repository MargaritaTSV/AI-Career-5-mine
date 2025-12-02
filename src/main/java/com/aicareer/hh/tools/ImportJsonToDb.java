package com.aicareer.hh.tools;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.hh.model.OutVacancy;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.repository.JdbcVacancyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Импорт существующего vacancies_all.json в PostgreSQL.
 * Работает с OutVacancy независимо от наличия геттеров.
 */
public class ImportJsonToDb {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Импорт JSON → PostgreSQL ===");

        DbConnectionProvider provider = new DbConnectionProvider();
        JdbcVacancyRepository repository = new JdbcVacancyRepository(provider);

        ObjectMapper mapper = new ObjectMapper();
        Path exportDir = Path.of("src/main/resources/export");
        Path source = resolveSourcePath(args, exportDir);

        System.out.println("Импорт из файла: " + source.toAbsolutePath());

        List<OutVacancy> outVacancies = mapper.readValue(
                source.toFile(),
                new TypeReference<List<OutVacancy>>() {}
        );

        // Преобразование OutVacancy → Vacancy
        List<Vacancy> vacancies = outVacancies.stream().map(o -> {
            Vacancy v = new Vacancy();

            // если поля публичные — прямое обращение
            try {
                v.setId((String) OutVacancy.class.getField("id").get(o));
                v.setTitle((String) OutVacancy.class.getField("title").get(o));
                v.setCompany((String) OutVacancy.class.getField("company").get(o));
                v.setCity((String) OutVacancy.class.getField("city").get(o));
                v.setExperience((String) OutVacancy.class.getField("experience").get(o));
                v.setEmployment((String) OutVacancy.class.getField("employment").get(o));
                v.setSchedule((String) OutVacancy.class.getField("schedule").get(o));
                v.setSalaryFrom((Integer) OutVacancy.class.getField("salaryFrom").get(o));
                v.setSalaryTo((Integer) OutVacancy.class.getField("salaryTo").get(o));
                v.setCurrency((String) OutVacancy.class.getField("currency").get(o));
                v.setDescription((String) OutVacancy.class.getField("description").get(o));
                v.setUrl((String) OutVacancy.class.getField("url").get(o));
                v.setSource((String) OutVacancy.class.getField("source").get(o));
                v.setPublishedAt((String) OutVacancy.class.getField("publishedAt").get(o));
                Object skillsObj = OutVacancy.class.getField("skills").get(o);
                if (skillsObj instanceof List<?> skillsList) {
                    v.setSkills((List<String>) skillsList);
                }
            } catch (Exception ignore) {
                // если нет нужного поля — просто пропускаем
            }
            return v;
        }).collect(Collectors.toList());

        repository.saveAll(vacancies);

        System.out.println("✅ Импорт JSON → БД завершён. Всего записей: " + vacancies.size());
    }

    private static Path resolveSourcePath(String[] args, Path exportDir) throws Exception {
        if (args.length > 0) {
            Path provided = Path.of(args[0]);
            Path resolved = provided.isAbsolute() ? provided : exportDir.resolve(provided);
            if (!Files.exists(resolved)) {
                throw new IllegalArgumentException(
                        "Указанный файл не найден: " + resolved.toAbsolutePath()
                );
            }
            return resolved;
        }

        try (var stream = Files.list(exportDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("vacancies_all_"))
                    .sorted()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Не найден ни один файл vacancies_all_* в " + exportDir.toAbsolutePath()
                    ));
        }
    }
}
