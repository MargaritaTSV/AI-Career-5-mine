package com.aicareer.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RoadmapPromptBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String USER_MATRIX_RESOURCE = "matrices/user_skill_matrix.json";
    private static final String DESIRED_MATRIX_RESOURCE = "matrices/desired_role_matrix.json";
    private static final String SKILL_GRAPH_RESOURCE = "graphs/skills-graph.json";
    private static final String VACANCIES_RESOURCE = "matrices/vacancies.json";

    private RoadmapPromptBuilder() {
    }

    public static void main(String[] args) {
        System.out.println(build());
    }

    public static String build() {
        Map<String, Integer> userMatrix = readSkillMatrix(USER_MATRIX_RESOURCE);
        Map<String, Integer> desiredMatrix = readSkillMatrix(DESIRED_MATRIX_RESOURCE);
        String graphJson = readResourceJson(SKILL_GRAPH_RESOURCE);
        String vacanciesJson = readResourceJson(VACANCIES_RESOURCE);

        List<String> userSkills = flaggedSkills(userMatrix);
        List<String> targetSkills = flaggedSkills(desiredMatrix);
        List<String> missingSkills = targetSkills.stream()
                .filter(skill -> !userSkills.contains(skill))
                .toList();

        return String.join("\n", List.of(
                "Ты — карьерный консультант и планировщик обучения. Используй модель deepseek-r1:8b.",
                "Данные ниже: матрица навыков пользователя, требования роли, ориентированный граф зависимостей навыков и список вакансий роли.",
                "Задача: выдать краткий маршрут, который быстрее всего закроет дефицит навыков для целевой роли; более сильные рёбра графа ставь раньше в очереди.",
                "\nТекущие навыки пользователя (1 = владеет):\n" + formatJson(userMatrix)
                        + "\nСильные стороны: " + String.join(", ", userSkills),
                "\nМатрица требуемых навыков роли:\n" + formatJson(desiredMatrix)
                        + "\nКлючевые цели: " + String.join(", ", targetSkills),
                "\nНавыки, которых не хватает: " + (missingSkills.isEmpty() ? "нет" : String.join(", ", missingSkills)),
                "\nОриентированный граф навыков (используй веса рёбер для приоритета шагов):\n" + graphJson,
                "\nВакансии для анализа стеков и узких тем (используй для выбора фреймворков и инструментов):\n" + vacanciesJson,
                "\nФормат ответа:",
                "Roadmap: 3–7 шагов. Каждый шаг — одно предложение вида 'Шаг 1. Java: ООП, коллекции → Шаг 2. Алгоритмы: асимптотика, разбиение.' или 'Шаг 2. Алгоритмы на Java: асимптотический анализ, толстое и тонкое разбиение.'",
                "Ответ заверши строкой: 'Итого вы будете уметь: … (перечень ключевых навыков)'.",
                "Правила: сначала закрывай пробелы в навыках, опираясь на уже освоенные; сильные рёбра графа = более важные зависимости, ставь их раньше; разрешено указывать фреймворки, темы, технологии; не повторяй уже освоенные навыки как отдельные шаги — только используй их как фундамент."
        ));

    }

    private static Map<String, Integer> readSkillMatrix(String resource) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resource);
            }
            return MAPPER.readValue(is, new TypeReference<LinkedHashMap<String, Integer>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read matrix resource: " + resource, e);
        }
    }

    private static String readResourceJson(String resource) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resource);
            }
            JsonNode node = MAPPER.readTree(is);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse JSON resource: " + resource, e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read JSON resource: " + resource, e);
        }
    }

    private static List<String> flaggedSkills(Map<String, Integer> matrix) {
        return matrix.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() == 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static String formatJson(Map<String, Integer> json) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to format JSON payload", e);
        }
    }
}
