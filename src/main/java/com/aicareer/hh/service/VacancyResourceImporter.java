package com.aicareer.hh.service;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.hh.model.OutVacancy;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.repository.JdbcVacancyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Imports all vacancy export files into the database so that they are available for further processing.
 */
public class VacancyResourceImporter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final JdbcVacancyRepository repository;
    private final Path exportDir;

    public VacancyResourceImporter(DbConnectionProvider provider) {
        this(provider, Path.of("src/main/resources/export"));
    }

    public VacancyResourceImporter(DbConnectionProvider provider, Path exportDir) {
        this.repository = new JdbcVacancyRepository(provider);
        this.exportDir = exportDir;
    }

    public void importAll() {
        if (!Files.isDirectory(exportDir)) {
            System.out.println("[VACANCY] Директория с экспортами не найдена: " + exportDir.toAbsolutePath());
            return;
        }

        List<Path> files = listVacancyFiles();
        if (files.isEmpty()) {
            System.out.println("[VACANCY] Не найдено файлов vacancies_* в " + exportDir.toAbsolutePath());
            return;
        }

        for (Path file : files) {
            importSingle(file);
        }
    }

    private List<Path> listVacancyFiles() {
        try (Stream<Path> stream = Files.list(exportDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("vacancies_") && name.endsWith(".json");
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать директорию: " + exportDir, e);
        }
    }

    private void importSingle(Path file) {
        try {
            List<OutVacancy> outVacancies = MAPPER.readValue(
                    file.toFile(),
                    new TypeReference<List<OutVacancy>>() {}
            );

            List<Vacancy> vacancies = outVacancies.stream()
                    .map(VacancyMapper::fromOutVacancy)
                    .filter(Objects::nonNull)
                    .peek(v -> {
                        v.setSource(file.getFileName().toString());
                    })
                    .toList();

            repository.saveAll(vacancies);
            System.out.println("✅ Импортировано из " + file.getFileName() + ": " + vacancies.size());
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать файл вакансий: " + file.toAbsolutePath(), e);
        }
    }
}
