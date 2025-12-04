package com.aicareer.hh.tools;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.hh.model.OutVacancy;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.repository.JdbcVacancyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VacancyResourceImporter {

    private static final Path DEFAULT_EXPORT_DIR = Path.of("src/main/resources/export");

    private final ObjectMapper mapper = new ObjectMapper();
    private final JdbcVacancyRepository repository;
    private final Path exportDir;

    public VacancyResourceImporter(DbConnectionProvider provider) {
        this(provider, DEFAULT_EXPORT_DIR);
    }

    public VacancyResourceImporter(DbConnectionProvider provider, Path exportDir) {
        this.repository = new JdbcVacancyRepository(provider);
        this.exportDir = exportDir;
    }

    public void importAllFromResources() {
        importAllFromDirectory(exportDir);
    }

    public void importAllFromDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            System.out.println("[VACANCY] Директория с вакансиями не найдена: " + directory.toAbsolutePath());
            return;
        }

        List<Path> vacancyFiles;
        try (Stream<Path> stream = Files.list(directory)) {
            vacancyFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isVacancyFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать каталог с вакансиями: " + directory, e);
        }

        if (vacancyFiles.isEmpty()) {
            System.out.println("[VACANCY] В каталоге нет файлов vacancies_*: " + directory.toAbsolutePath());
            return;
        }

        for (Path file : vacancyFiles) {
            importFile(file);
        }
    }

    public void importFile(Path file) {
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Файл с вакансиями не найден: " + file.toAbsolutePath());
        }

        System.out.println("[VACANCY] Импортируем вакансии из " + file.toAbsolutePath());

        List<OutVacancy> outVacancies;
        try {
            outVacancies = mapper.readValue(file.toFile(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать файл вакансий: " + file, e);
        }

        List<Vacancy> vacancies = outVacancies.stream()
                .filter(Objects::nonNull)
                .map(this::toVacancy)
                .collect(Collectors.toList());

        repository.saveAll(vacancies);
    }

    private boolean isVacancyFile(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith("vacancies_") && name.endsWith(".json");
    }

    private Vacancy toVacancy(OutVacancy source) {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(source.id);
        vacancy.setTitle(source.title);
        vacancy.setCompany(source.company);
        vacancy.setCity(source.city);
        vacancy.setExperience(source.experience);
        vacancy.setEmployment(source.employment);
        vacancy.setSchedule(source.schedule);
        vacancy.setSalaryFrom(source.salaryFrom);
        vacancy.setSalaryTo(source.salaryTo);
        vacancy.setCurrency(source.currency);
        vacancy.setSkills(source.skills);
        vacancy.setDescription(source.description);
        vacancy.setUrl(source.url);
        vacancy.setSource(source.source);
        vacancy.setPublishedAt(source.publishedAt);
        vacancy.setScore(0);
        return vacancy;
    }
}

