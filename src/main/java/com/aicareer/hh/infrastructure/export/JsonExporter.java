package com.aicareer.hh.infrastructure.export;

import com.aicareer.hh.model.OutVacancy;
import com.aicareer.hh.model.Vacancy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public final class JsonExporter {
    private static final Path EXPORT_DIR = Path.of("src/main/resources/export");

    private final ObjectMapper om = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public void writeJson(Collection<Vacancy> items, String fileName) {
        try {
            List<OutVacancy> out = items.stream()
                    .map(OutVacancy::from)
                    .toList();
            Files.createDirectories(EXPORT_DIR);
            Path output = EXPORT_DIR.resolve(fileName);
            om.writeValue(output.toFile(), out);
            System.out.println("Сохранено: " + output.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("JSON export failed: " + e.getMessage(), e);
        }
    }
}
