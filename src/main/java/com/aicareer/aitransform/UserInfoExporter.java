package com.aicareer.aitransform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.db.Database;
import org.example.profile.Profile;
import org.example.profile.ProfileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class UserInfoExporter {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final Path EXPORT_DIR = Path.of("src/main/resources/export");
    private static final Path OUTPUT_FILE = Path.of("src/main/resources/matrices/vacancies.json");
    private static final Path USER_MATRIX_FILE = Path.of("src/main/resources/matrices/user_skill_matrix.json");
    private static final String FILE_PREFIX = "vacancies_top25_";
    private static final String FILE_SUFFIX = ".json";

    private UserInfoExporter() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: UserInfoExporter <user-id>");
            System.exit(1);
        }
        Profile profile = exportUserData(args[0]);
        System.out.printf("Vacancies and skills exported for user %s (%s)%n", args[0], profile.getTargetRole());
    }

    public static Profile exportUserData(String userId) {
        Database.init();
        Profile profile = loadProfile(userId);

        Path source = resolveVacancyFile(profile.getTargetRole());
        copyVacancyFile(source);
        writeUserMatrix(profile.getSkills());

        return profile;
    }

    private static Profile loadProfile(String userId) {
        try {
            ProfileRepository repository = new ProfileRepository();
            return repository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Не удалось найти профиль пользователя " + userId + ". Сохраните профиль перед запуском."));
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SQLException sqlException) {
                throw new IllegalStateException("Не удалось прочитать профиль пользователя", sqlException);
            }
            throw e;
        }
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

    private static void writeUserMatrix(Map<String, Integer> skills) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        skills.forEach((skill, level) -> normalized.put(skill, level != null && level > 0 ? 1 : 0));

        try {
            Files.createDirectories(USER_MATRIX_FILE.getParent());
            MAPPER.writeValue(USER_MATRIX_FILE.toFile(), normalized);
            System.out.println("User skills saved to: " + USER_MATRIX_FILE.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить навыки пользователя", e);
        }
    }
}
