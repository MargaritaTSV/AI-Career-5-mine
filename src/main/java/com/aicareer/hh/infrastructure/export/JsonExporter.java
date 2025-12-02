package com.aicareer.hh.infrastructure.export;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

public class JsonExporter {

    // ОДНО место для всех JSON-файлов
    private static final Path EXPORT_DIR = Path.of("src", "main", "resources", "export");

    private final ObjectMapper mapper = new ObjectMapper();

    public <T> void writeJson(Collection<T> data, String fileName) {
        try {
            // создаём папку src/main/resources/export, если её ещё нет
            Files.createDirectories(EXPORT_DIR);

            // соберём полный путь к файлу внутри resources/export
            Path outputFile = EXPORT_DIR.resolve(fileName);

            mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(outputFile.toFile(), data);

            System.out.println("✅ JSON сохранён: " + outputFile.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при записи JSON: " + e.getMessage(), e);
        }
    }
}