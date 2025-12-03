package org.example;

import com.aicareer.aitransform.SkillsExtraction;
import com.aicareer.aitransform.UserInfoExporter;
import com.aicareer.comparison.Comparison;
import com.aicareer.recommendation.DeepseekRoadmapClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.db.Database;
import org.example.db.TestDataSeeder;
import org.example.profile.Profile;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkflowLauncher {
    private static final Path VACANCIES_MATRIX = Path.of("src/main/resources/matrices/vacancies.json");
    private static final Path DESIRED_MATRIX = Path.of("src/main/resources/matrices/desired_role_matrix.json");
    private static final Path STATUS_OUTPUT = Path.of("src/main/resources/matrices/skill_comparison.json");
    private static final Path SUMMARY_OUTPUT = Path.of("src/main/resources/matrices/summary.json");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        Database.init();

        String userId = args.length > 0 ? args[0] : TestDataSeeder.seedAndGetDefaultUser();
        System.out.println("[INIT] Работаем с пользователем: " + userId);

        Profile profile = UserInfoExporter.exportUserData(userId);

        Map<String, Integer> desiredRoleMatrix = SkillsExtraction.fromFile(VACANCIES_MATRIX);
        writeMatrix(DESIRED_MATRIX, desiredRoleMatrix);

        Map<String, Integer> userSkills = normalizeSkills(profile.getSkills());

        Comparison.ComparisonResult comparisonResult = Comparison.compareAndSave(
                desiredRoleMatrix,
                userSkills,
                STATUS_OUTPUT,
                SUMMARY_OUTPUT
        );

        printSummary(comparisonResult);
        printRoadmap();
    }

    private static Map<String, Integer> normalizeSkills(Map<String, Integer> skills) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        skills.forEach((skill, level) -> normalized.put(skill, level != null && level > 0 ? 1 : 0));
        return normalized;
    }

    private static void writeMatrix(Path path, Map<String, Integer> matrix) {
        try {
            if (path.getParent() != null) {
                path.getParent().toFile().mkdirs();
            }
            MAPPER.writeValue(path.toFile(), matrix);
            System.out.println("[MATRIX] Saved to: " + path.toAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сохранить матрицу навыков роли", e);
        }
    }

    private static void printSummary(Comparison.ComparisonResult result) {
        Map<String, List<String>> summary = result.summary();
        List<String> strong = summary.getOrDefault("соответствует", List.of());
        List<String> weak = summary.getOrDefault("требует улучшения", List.of());
        List<String> exceeds = summary.getOrDefault("лучше ожидаемого", List.of());

        System.out.println("\n[SUMMARY] Сильные стороны: " + (strong.isEmpty() ? "нет" : String.join(", ", strong)));
        System.out.println("[SUMMARY] Требуют прокачки: " + (weak.isEmpty() ? "нет" : String.join(", ", weak)));
        if (!exceeds.isEmpty()) {
            System.out.println("[SUMMARY] Уже выше требований: " + String.join(", ", exceeds));
        }
    }

    private static void printRoadmap() {
        try {
            String roadmap = DeepseekRoadmapClient.generateRoadmap();
            System.out.println("\n[AI] Рекомендации по обучению и достижению цели:\n" + roadmap);
        } catch (Exception e) {
            System.err.println("[AI] Не удалось получить ответ от модели: " + e.getMessage());
            System.err.println("[AI] Убедитесь, что Ollama запущен и переменная OLLAMA_HOST указывает на локальный сервис.");
        }
    }
}
