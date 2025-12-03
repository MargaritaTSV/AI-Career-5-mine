package com.aicareer;

import com.aicareer.aitransform.AppDatabaseInitializer;
import com.aicareer.aitransform.SkillsExtraction;
import com.aicareer.aitransform.UserInfoExporter;
import com.aicareer.comparison.Comparison;
import com.aicareer.comparison.Comparison.ComparisonResult;
import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.recommendation.DeepseekRoadmapClient;
import com.aicareer.recommendation.RoadmapPromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * End-to-end runner for the career workflow:
 * 1) prepares DB schema and seed users,
 * 2) loads a user's skills from DB,
 * 3) extracts role requirements from the vacancies_top25 JSON,
 * 4) compares user vs. role,
 * 5) asks the model for a roadmap.
 */
public class AppRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final Path USER_MATRIX_PATH = Path.of("src/main/resources/matrices/user_skill_matrix.json");
    private static final Path ROLE_MATRIX_PATH = Path.of("src/main/resources/matrices/desired_role_matrix.json");
    private static final Path STATUSES_PATH = Path.of("src/main/resources/matrices/skill_comparison.json");
    private static final Path SUMMARY_PATH = Path.of("src/main/resources/matrices/summary.json");

    public static void main(String[] args) {
        String email = args.length > 0 ? args[0] : "test@example.com";
        String roleOverride = args.length > 1 ? args[1] : null;

        DbConnectionProvider provider = new DbConnectionProvider();

        System.out.println("[DB] applying schema and seeds...");
        new AppDatabaseInitializer(provider).applySchemaAndData();

        UserInfoExporter exporter = new UserInfoExporter(provider);
        UserInfoExporter.ProfileSnapshot profile = exporter.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        String targetRole = roleOverride != null && !roleOverride.isBlank()
                ? roleOverride
                : profile.targetRole();
        if (targetRole == null || targetRole.isBlank()) {
            throw new IllegalStateException("Target role is empty for user: " + email);
        }

        System.out.println("[USER] loaded profile for: " + profile.name() + " (" + targetRole + ")");

        exporter.writeUserSkillMatrix(profile.skills(), USER_MATRIX_PATH);

        String vacanciesResource = resolveVacanciesResource(targetRole);
        System.out.println("[ROLE] using vacancies resource: " + vacanciesResource);

        Map<String, Integer> roleMatrix = SkillsExtraction.fromResource(vacanciesResource);
        writeJson(ROLE_MATRIX_PATH, roleMatrix);

        ComparisonResult comparison = Comparison.calculate(roleMatrix, profile.skills());
        Comparison.writeOutputs(comparison, STATUSES_PATH, SUMMARY_PATH);

        System.out.println("[COMPARE] Strong sides: " + comparison.summary().getOrDefault("лучше ожидаемого", List.of()));
        System.out.println("[COMPARE] Weak sides:   " + comparison.summary().getOrDefault("требует улучшения", List.of()));

        String prompt = RoadmapPromptBuilder.build(
                vacanciesResource,
                "matrices/user_skill_matrix.json",
                "matrices/desired_role_matrix.json",
                "graphs/skills-graph.json"
        );

        try {
            String roadmap = DeepseekRoadmapClient.generateRoadmap(prompt);
            System.out.println("\n[AI RESPONSE]\n" + roadmap);
        } catch (Exception e) {
            System.err.println("[AI] Failed to get roadmap from model: " + e.getMessage());
        }
    }

    private static String resolveVacanciesResource(String roleName) {
        String safe = roleName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String resource = "export/vacancies_top25_" + safe + ".json";

        if (resourceExists(resource)) {
            return resource;
        }
        throw new IllegalArgumentException("No vacancies_top25 resource found for role: " + roleName);
    }

    private static boolean resourceExists(String resource) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResource(resource) != null;
    }

    private static void writeJson(Path path, Object payload) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            MAPPER.writeValue(path.toFile(), payload);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write JSON to " + path, e);
        }
    }
}
