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
    private static final String VACANCIES_RESOURCE = "export/vacancies_top25_java_backend_developer.json";
    private static final String RESOURCES_FOR_RECOMMENDATIONS = """
            ## Java

            * Введение в программирование на Java — Stepik (0)
            * Core Java — книга Хорстманн (1)

            ## C++

            * Современный C++ — Stepik (1)
            * The C++ Programming Language — книга Страуструп (2)

            ## Python

            * Python for Everybody — Coursera (0)
            * Learning Python — книга Марк Лутц (1)

            ## JavaScript

            * JavaScript для начинающих — Stepik (0)
            * Eloquent JavaScript — книга Хавербек (1)

            ## SQL

            * Интерактивный тренажёр по SQL — Stepik (0)
            * SQL за 10 минут — книга Бен Форта (0)

            ## Docker

            * Docker: основы контейнеризации — Stepik (1)
            * Docker Documentation — официальная документация (2)

            ## C#

            * Основы C# — Stepik (0)
            * C# 8.0 и .NET Core — книга Троелсен (2)

            ## PHP

            * Основы PHP — Stepik (0)
            * PHP Objects, Patterns, and Practice — книга (1)

            ## Spring

            * Spring Framework от JetBrains — Coursera (1)
            * Spring Framework Documentation — официальная документация (2)

            ## Machine Learning

            * Machine Learning (Andrew Ng) — Coursera (1)
            * Hands-On Machine Learning — книга Géron (2)

            ## React

            * ReactJS: основы разработки — Stepik (1)
            * React Documentation — официальная документация (1)

            ## TypeScript

            * TypeScript для начинающих — Stepik (0)
            * TypeScript Documentation — официальная документация (1)

            ## Kubernetes

            * Architecting with Kubernetes — Coursera (2)
            * Kubernetes Documentation — официальная документация (2)

            ## Terraform

            * Terraform for Beginners — Coursera/Udemy (1)
            * Terraform Documentation — официальная документация (2)

            ## Linux

            * Linux для начинающих — Stepik (0)
            * The Linux Command Line — книга (1)

            ## Hibernate

            * Hibernate & JPA Fundamentals — Udemy (1)
            * Hibernate ORM Documentation — официальная документация (2)

            ## Apache Spark

            * Big Data Analysis with Scala and Spark — Coursera (2)
            * Apache Spark Documentation — официальная документация (2)

            ## Pandas

            * Pandas: обработка данных в Python — Stepik (1)
            * Pandas Documentation — официальная документация (1)

            ## Kafka

            * Apache Kafka for Beginners — Coursera/Udemy (1)
            * Apache Kafka Documentation — официальная документация (2)

            ## AWS

            * AWS Cloud Practitioner Essentials — Coursera (1)
            * AWS Documentation — официальная документация (2)
            """;

    private RoadmapPromptBuilder() {
    }

    public static void main(String[] args) {
        String vacanciesResource = args.length > 0 ? args[0] : VACANCIES_RESOURCE;
        System.out.println(build(vacanciesResource));
    }

    public static String build() {
        return build(VACANCIES_RESOURCE, USER_MATRIX_RESOURCE, DESIRED_MATRIX_RESOURCE, SKILL_GRAPH_RESOURCE);
    }

    public static String build(String vacanciesResource) {
        return build(vacanciesResource, USER_MATRIX_RESOURCE, DESIRED_MATRIX_RESOURCE, SKILL_GRAPH_RESOURCE);
    }

    public static String build(String vacanciesResource,
                               String userMatrixResource,
                               String desiredMatrixResource,
                               String skillGraphResource) {
        Map<String, Integer> userMatrix = readSkillMatrix(userMatrixResource);
        Map<String, Integer> desiredMatrix = readSkillMatrix(desiredMatrixResource);
        String graphJson = readResourceJson(skillGraphResource);
        String vacanciesJson = readResourceJson(vacanciesResource);

        List<String> userSkills = flaggedSkills(userMatrix);
        List<String> targetSkills = flaggedSkills(desiredMatrix);
        List<String> missingSkills = targetSkills.stream()
                .filter(skill -> !userSkills.contains(skill))
                .toList();

        return String.join("\n", List.of(
                "Ты — карьерный консультант и планировщик обучения. Используй модель deepseek-r1:8b.",
                "Данные ниже: матрица навыков пользователя, требования роли, ориентированный граф зависимостей навыков, список вакансий роли и список учебных ресурсов.",
                "Задача: выдать краткий маршрут, который быстрее всего закроет дефицит навыков для целевой роли; более сильные рёбра графа ставь раньше в очереди.",
                "Для каждого шага выбери 1–2 ресурса из предложенного списка, которые лучше всего подходят под темы шага, и укажи их явно.",
                "\nТекущие навыки пользователя (1 = владеет):\n" + formatJson(userMatrix)
                        + "\nСильные стороны: " + String.join(", ", userSkills),
                "\nМатрица требуемых навыков роли:\n" + formatJson(desiredMatrix)
                        + "\nКлючевые цели: " + String.join(", ", targetSkills),
                "\nНавыки, которых не хватает: " + (missingSkills.isEmpty() ? "нет" : String.join(", ", missingSkills)),
                "\nОриентированный граф навыков (используй веса рёбер для приоритета шагов):\n" + graphJson,
                "\nВакансии для анализа стеков и узких тем (используй для выбора фреймворков и инструментов):\n" + vacanciesJson,
                "\nСписок ресурсов: используй только их, подбирая к каждому шагу 1–2 варианта:\n" + RESOURCES_FOR_RECOMMENDATIONS,
                "\nФормат ответа:",
                "Roadmap: 3–7 шагов. Каждый шаг — одно предложение вида 'Шаг 1. Python: Pandas, Matplotlib, NumPy. Ресурсы: Learning Python — Марк Лутц; Pandas Documentation.'",
                "Ответ заверши строкой: 'Итого вы будете уметь: … (перечень ключевых навыков)'.",
                "Правила: сначала закрывай пробелы в навыках, опираясь на уже освоенные; сильные рёбра графа = более важные зависимости, ставь их раньше; разрешено указывать фреймворки, темы, технологии; не повторяй уже освоенные навыки как отдельные шаги — только используй их как фундамент; для каждого шага перечисли 1–2 подходящих ресурса из списка выше."
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
