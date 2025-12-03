package com.aicareer.recommendation;

import org.example.db.Database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.stream.Stream;

public final class UserInfoExporter {
    private static final Path EXPORT_DIR = Path.of("src/main/resources/export");
    private static final Path OUTPUT_FILE = Path.of("src/main/resources/matrices/vacancies.json");
    private static final String FILE_PREFIX = "vacancies_top25_";
    private static final String FILE_SUFFIX = ".json";

    private UserInfoExporter() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: UserInfoExporter <user-id>");
            System.exit(1);
        }
        exportVacancies(args[0]);
    }

    public static void exportVacancies(String userId) {
        Database.init();
        String desiredRole = loadDesiredRole(userId);
        Path source = resolveVacancyFile(desiredRole);
        copyVacancyFile(source);
    }

    private static String loadDesiredRole(String userId) {
        String sql = "SELECT target_role FROM user_profiles WHERE user_id = ?";
        try (Connection connection = Database.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("target_role");
                    if (role != null && !role.isBlank()) {
                        return role;
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось прочитать профиль пользователя", e);
        }
        throw new IllegalArgumentException("Не удалось найти желаемую роль для пользователя " + userId);
    }

    private static Path resolveVacancyFile(String desiredRole) {
        String normalizedRole = desiredRole
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        Path candidate = EXPORT_DIR.resolve(FILE_PREFIX + normalizedRole + FILE_SUFFIX);
        if (Files.exists(candidate)) {
            return candidate;
        }

        String availableRoles = availableRoleSuffixes();
        throw new IllegalStateException("Файл вакансий не найден для роли '" + desiredRole + "'. Доступные роли: " + availableRoles);
    }

    private static String availableRoleSuffixes() {
        try (Stream<Path> stream = Files.list(EXPORT_DIR)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX))
                    .map(name -> name.substring(FILE_PREFIX.length(), name.length() - FILE_SUFFIX.length()))
                    .sorted()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось получить список файлов вакансий", e);
        }
    }

    private static void copyVacancyFile(Path source) {
        try {
            Files.createDirectories(OUTPUT_FILE.getParent());
            Files.copy(source, OUTPUT_FILE, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Vacancies saved to: " + OUTPUT_FILE.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить вакансии в матрицу", e);
        }
    }
}
