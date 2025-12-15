package com.aicareer.hh.tools;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Утилита для импорта локальных файлов вакансий в PostgreSQL.
 * Без аргументов импортирует все vacancies_*.json из каталога export.
 */
public class ImportJsonToDb {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Импорт JSON → PostgreSQL ===");

        DbConnectionProvider provider = new DbConnectionProvider();
        VacancyResourceImporter importer = new VacancyResourceImporter(provider);

        Path exportDir = Path.of("src/main/resources/export");

        if (args.length == 0) {
            System.out.println("Импорт всех файлов vacancies_* из каталога: " + exportDir.toAbsolutePath());
            importer.importAllFromDirectory(exportDir);
            return;
        }

        Path source = resolveSourcePath(args, exportDir);
        importer.importFile(source);
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
