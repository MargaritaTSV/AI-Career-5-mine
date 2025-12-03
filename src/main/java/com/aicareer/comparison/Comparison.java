package com.aicareer.comparison;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Comparison {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final String STATUS_MATCH = "соответствует";
    private static final String STATUS_IMPROVE = "требует улучшения";
    private static final String STATUS_EXCEEDS = "лучше ожидаемого";

    private static final Path DEFAULT_ROLE_MATRIX =
            Path.of("src/main/resources/matrices/desired_role_matrix.json");
    private static final Path DEFAULT_USER_MATRIX =
            Path.of("src/main/resources/matrices/user_skill_matrix.json");
    private static final Path DEFAULT_STATUS_OUTPUT =
            Path.of("src/main/resources/matrices/skill_comparison.json");
    private static final Path DEFAULT_SUMMARY_OUTPUT =
            Path.of("src/main/resources/matrices/summary.json");

    public static void main(String[] args) {
        Path roleMatrixPath = args.length > 0 ? Path.of(args[0]) : DEFAULT_ROLE_MATRIX;
        Path userMatrixPath = args.length > 1 ? Path.of(args[1]) : DEFAULT_USER_MATRIX;
        Path statusOutputPath = args.length > 2 ? Path.of(args[2]) : DEFAULT_STATUS_OUTPUT;
        Path summaryOutputPath = args.length > 3 ? Path.of(args[3]) : DEFAULT_SUMMARY_OUTPUT;

        Map<String, Integer> roleMatrix = readMatrix(roleMatrixPath);
        Map<String, Integer> userMatrix = readMatrix(userMatrixPath);

        ComparisonResult result = calculate(roleMatrix, userMatrix);
        writeOutputs(result, statusOutputPath, summaryOutputPath);

        System.out.println("Skill comparison saved to: " + statusOutputPath.toAbsolutePath());
        System.out.println("Summary saved to: " + summaryOutputPath.toAbsolutePath());
    }

    public static ComparisonResult calculate(Map<String, Integer> roleMatrix,
                                             Map<String, Integer> userMatrix) {
        Map<String, String> statuses = compareMatrices(roleMatrix, userMatrix);
        Map<String, List<String>> summary = buildSummary(statuses);
        return new ComparisonResult(roleMatrix, userMatrix, statuses, summary);
    }

    public static void writeOutputs(ComparisonResult result, Path statusOutputPath, Path summaryOutputPath) {
        writeJson(statusOutputPath, result.statuses());
        writeJson(summaryOutputPath, result.summary());
    }

    private static Map<String, Integer> readMatrix(Path path) {
        try {
            return MAPPER.readValue(path.toFile(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read matrix from: " + path, e);
        }
    }

    private static Map<String, String> compareMatrices(
            Map<String, Integer> roleMatrix, Map<String, Integer> userMatrix) {
        Set<String> allSkills = new LinkedHashSet<>(roleMatrix.keySet());
        allSkills.addAll(userMatrix.keySet());

        Map<String, String> statuses = new LinkedHashMap<>();
        for (String skill : allSkills) {
            int desired = roleMatrix.getOrDefault(skill, 0);
            int user = userMatrix.getOrDefault(skill, 0);
            String status;
            if (user == desired) {
                status = STATUS_MATCH;
            } else if (user < desired) {
                status = STATUS_IMPROVE;
            } else {
                status = STATUS_EXCEEDS;
            }
            statuses.put(skill, status);
        }
        return statuses;
    }

    private static Map<String, List<String>> buildSummary(Map<String, String> statuses) {
        Map<String, List<String>> summary = new LinkedHashMap<>();
        summary.put(STATUS_MATCH, new ArrayList<>());
        summary.put(STATUS_IMPROVE, new ArrayList<>());
        summary.put(STATUS_EXCEEDS, new ArrayList<>());

        statuses.forEach((skill, status) -> summary.computeIfAbsent(status, k -> new ArrayList<>()).add(skill));
        return summary;
    }

    private static void writeJson(Path outputPath, Object payload) {
        try {
            Files.createDirectories(outputPath.toAbsolutePath().getParent());
            MAPPER.writeValue(outputPath.toFile(), payload);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write JSON to: " + outputPath, e);
        }
    }

    public record ComparisonResult(
            Map<String, Integer> roleMatrix,
            Map<String, Integer> userMatrix,
            Map<String, String> statuses,
            Map<String, List<String>> summary
    ) {
    }
}
