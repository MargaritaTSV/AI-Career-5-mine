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

    private RoadmapPromptBuilder() {
    }

    public static void main(String[] args) {
        System.out.println(build());
    }

    public static String build() {
        Map<String, Integer> userMatrix = readSkillMatrix(USER_MATRIX_RESOURCE);
        Map<String, Integer> desiredMatrix = readSkillMatrix(DESIRED_MATRIX_RESOURCE);
        String graphJson = readResourceJson(SKILL_GRAPH_RESOURCE);

        List<String> userSkills = flaggedSkills(userMatrix);
        List<String> targetSkills = flaggedSkills(desiredMatrix);
        List<String> missingSkills = targetSkills.stream()
                .filter(skill -> !userSkills.contains(skill))
                .toList();

        return String.join("\n", List.of(
                "Ты — карьерный консультант и планировщик обучения. Используй модель deepseek-r1:8b.",
                "Ниже данные: матрица навыков пользователя, требования роли и ориентированный граф зависимостей навыков.",
                "Задача: на основе графа выдать конкретный и краткий маршрут, который быстрее всего ведёт к роли." +
                        " Вес ребра = сила зависимости, ставь сильные переходы раньше.",
                "\nТекущие навыки пользователя (1 = владеет):\n" + formatJson(userMatrix)
                        + "\nСильные стороны: " + String.join(", ", userSkills),
                "\nМатрица требуемых навыков роли (1 = критично важно):\n" + formatJson(desiredMatrix)
                        + "\nКлючевые цели: " + String.join(", ", targetSkills),
                "\nНавыки, которых не хватает: " + (missingSkills.isEmpty() ? "нет" : String.join(", ", missingSkills)),
                "\nОриентированный граф навыков (используй веса рёбер для приоритета шагов):\n" + graphJson,
                "\nСформируй ответ в двух частях без Markdown и без лишних комментариев:",
                "1) Roadmap — 6–9 шагов вида 'Шаг 1. Java: ООП, коллекции → Шаг 2. Алгоритмы и структуры данных'." +
                        " В каждом шаге укажи конкретные темы/фреймворки/разделы и покажи зависимость ('на базе X изучи Y').",
                "2) Краткое объяснение очередности (2–3 предложения максимум): что учим первым, что вторым, что далее.",
                "Заверши строкой 'Итого вы будете обладать:' и перечисли ключевые навыки после прохождения шагов.",
                "Правила:" +
                        "\n- Сначала закрывай пробелы (missingSkills), опираясь на уже освоенные навыки." +
                        "\n- Каждый шаг = одно предложение; максимум укажи, на базе чего строится следующий навык." +
                        "\n- Разрешено перечислять фреймворки и разделы (например, Spring, Docker, SQL оптимизация, алгоритмы)." +
                        "\n- Не повторяй освоенные навыки как отдельные шаги, только используй их как основу."
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
