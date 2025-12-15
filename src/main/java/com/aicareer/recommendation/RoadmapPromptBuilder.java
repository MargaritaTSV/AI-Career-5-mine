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
            ## Computer Science (Harvard)

            * CS50: Introduction to Computer Science — Harvard/edX (2)
            * CS50's Introduction to Programming with Python — Harvard/edX (2)

            ## Java

            * Effective Java — Джошуа Блоха (2)
            
           Официальные туториалы Oracle – Java Tutorials
           Отличное место, чтобы закрыть синтаксис, ООП, коллекции, исключения.
       
        
           Книга: Кей Хорстманн — “Java. Библиотека профессионала” (Core Java)
           Толстая, но очень системная книга: язык, коллекции, многопоточность, базовая работа с сетью, JDBC.
            
            
            ## Python

            * Learning Python — книга Марк Лутц (1)
            * Fluent Python — книга Лучано Рамальо (2)

            ## JavaScript

            * Eloquent JavaScript — книга Хавербек (1)
            * You Don't Know JS Yet — серия книг Кайла Симпсона (2)

            ## SQL и данные

            * SQL за 10 минут — книга Бен Форта (0)
            * Designing Data-Intensive Applications — книга Клеппмана (2)

            ## Docker

            * Docker Deep Dive — книга Найджел Поултон (1)
            * Docker Documentation — официальная документация (2)
            
            ## 2. C++
        
         1. cppreference
            Основной справочник по современному C++ (C++11/14/17/20), стандартная библиотека, контейнеры, алгоритмы.
            [https://en.cppreference.com/](https://en.cppreference.com/)
        
         2. Курс Яндекс / МФТИ “Основы C++”
            Хорошо объясняет базу: указатели, память, ООП, STL.
            [https://www.coursera.org/learn/c-plus-plus-basics](https://www.coursera.org/learn/c-plus-plus-basics)
        
         3. Книга: Бьярне Страуструп — “Программирование. Принципы и практика использования C++”
            Для вдумчивого прохождения языка с нуля до уверенного уровня.

            ## Spring

            * Spring in Action — книга Крэйг Уоллс (1)
            * Spring Framework Documentation — официальная документация (2)

            ## Machine Learning

            * Hands-On Machine Learning — книга Géron (2)
            * Pattern Recognition and Machine Learning — книга Бишоп (2)

            ## React

            * React Documentation — официальная документация (1)
            * Epic React — курс от Kent C. Dodds (2)

            ## TypeScript

            * TypeScript Documentation — официальная документация (1)
            * Programming TypeScript — книга Борис Чёрни (2)

            ## Kubernetes

            * The Kubernetes Book — книга Найджел Поултон (1)
            * Kubernetes Documentation — официальная документация (2)

            ## Terraform

            * Terraform: Up & Running — книга Евгений Брикман (2)
            * Terraform Documentation — официальная документация (2)

            ## Linux

            * The Linux Command Line — книга (1)
            * How Linux Works — книга Брайан Уорд (2)

            ## Hibernate

            * Hibernate in Action — книга Бауэр, Кинг (1)
            * Hibernate ORM Documentation — официальная документация (2)

            ## Apache Spark

            * Learning Spark — книга Холдена Карау (2)
            * Apache Spark Documentation — официальная документация (2)

            ## Pandas

            * Pandas Documentation — официальная документация (1)
            * Effective Pandas — книга Мэтт Харрисон (2)

            ## Kafka

            * Kafka: The Definitive Guide — книга Неха Нархеде (2)
            * Apache Kafka Documentation — официальная документация (2)

            ## AWS

            * AWS Certified Solutions Architect Official Study Guide — книга (2)
            * AWS Documentation — официальная документация (2)
            
            И вот второй каталог материалов: 
                        ## Java
        
                       * Введение в программирование на Java — Stepik (уровень: лёгкий)
                       * Core Java, Vol. I–II — Cay Horstmann (книга, уровень: средний)
        
                       ## C++
        
                       * Современный C++ — Stepik (уровень: средний)
                       * The C++ Programming Language — Bjarne Stroustrup (книга, уровень: сложный)
        
                       ## Python
        
                       * Python for Everybody — Coursera (уровень: лёгкий)
                       * CS50's Introduction to Programming with Python — Harvard / edX (уровень: средний)
                       * Learning Python — Mark Lutz (книга, уровень: средний)
        
                       ## JavaScript
        
                       * JavaScript для начинающих — Stepik (уровень: лёгкий)
                       * CS50's Web Programming with Python and JavaScript — Harvard / edX (уровень: средний)
                       * Eloquent JavaScript — Marijn Haverbeke (книга, уровень: средний)
        
                       ## CS / Computer Science fundamentals
        
                       * CS50: Introduction to Computer Science — Harvard / edX (уровень: средний)
                       * Algorithms, Part I — Princeton / Coursera (уровень: сложный)
        
                       ## SQL
        
                       * Интерактивный тренажёр по SQL — Stepik (уровень: лёгкий)
                       * SQL за 10 минут — Ben Forta (книга, уровень: лёгкий)
        
                       ## Docker
        
                       * Docker: основы контейнеризации — Stepik (уровень: средний)
                       * Docker Documentation — официальная документация (уровень: средний)
        
                       ## C#
        
                       * Основы C# — Stepik (уровень: лёгкий)
                       * C# 8.0 и .NET Core — Andrew Troelsen (книга, уровень: сложный)
        
                       ## PHP
        
                       * Основы PHP — Stepik (уровень: лёгкий)
                       * PHP Objects, Patterns, and Practice — M. Zandstra (книга, уровень: средний)
        
                       ## Spring
        
                       * Spring Framework от JetBrains — Coursera (уровень: средний)
                       * Spring Framework Documentation — официальная документация (уровень: средний)
        
                       ## Machine Learning
        
                       * Machine Learning — Andrew Ng, Coursera (уровень: средний)
                       * Hands-On Machine Learning with Scikit-Learn, Keras, and TensorFlow — Aurélien Géron (книга, уровень: сложный)
        
                       ## React
        
                       * ReactJS: основы разработки — Stepik (уровень: средний)
                       * React Documentation — официальная документация (уровень: средний)
        
                       ## TypeScript
        
                       * TypeScript для начинающих — Stepik (уровень: лёгкий)
                       * TypeScript Documentation — официальная документация (уровень: средний)
        
                       ## Kubernetes
        
                       * Architecting with Kubernetes — Coursera (уровень: сложный)
                       * Kubernetes Documentation — официальная документация (уровень: сложный)
        
                       ## Terraform
        
                       * Terraform for Beginners — Coursera/Udemy (уровень: средний)
                       * Terraform Documentation — официальная документация (уровень: сложный)
        
                       ## Linux
                      * Linux для начинающих — Stepik (уровень: лёгкий)
                       * The Linux Command Line — William Shotts (книга, уровень: средний)
        
                       ## Hibernate
        
                       * Hibernate & JPA Fundamentals — Udemy (уровень: средний)
                       * Hibernate ORM Documentation — официальная документация (уровень: средний)
        
                       ## Apache Spark
        
                       * Big Data Analysis with Scala and Spark — Coursera (уровень: сложный)
                       * Apache Spark Documentation — официальная документация (уровень: сложный)
        
                       ## Pandas
        
                       * Pandas: обработка данных в Python — Stepik (уровень: средний)
                       * Pandas Documentation — официальная документация (уровень: средний)
        
                       ## Kafka
        
                       * Apache Kafka for Beginners — Coursera/Udemy (уровень: средний)
                       * Apache Kafka Documentation — официальная документация (уровень: сложный)
        
                       ## AWS
        
                       * AWS Cloud Practitioner Essentials — Coursera (уровень: средний)
                       * AWS Documentation — официальная документация (уровень: средний)
                       ""\";
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

        List<String> masteredSkills = flaggedSkills(userMatrix);
        List<String> requiredSkills = flaggedSkills(desiredMatrix);

        return build(
                vacanciesResource,
                userMatrixResource,
                desiredMatrixResource,
                skillGraphResource,
                masteredSkills,
                requiredSkills
        );
    }

    public static String build(String vacanciesResource,
                               String userMatrixResource,
                               String desiredMatrixResource,
                               String skillGraphResource,
                               List<String> masteredSkills,
                               List<String> requiredSkills) {
        Map<String, Integer> userMatrix = readSkillMatrix(userMatrixResource);
        Map<String, Integer> desiredMatrix = readSkillMatrix(desiredMatrixResource);
        String graphJson = readResourceJson(skillGraphResource);
        String vacanciesJson = readResourceJson(vacanciesResource);

        List<String> safeMastered = masteredSkills != null ? masteredSkills : flaggedSkills(userMatrix);
        List<String> safeRequired = requiredSkills != null ? requiredSkills : flaggedSkills(desiredMatrix);
        List<String> missingSkills = safeRequired.stream()
                .filter(skill -> !safeMastered.contains(skill))
                .toList();

        return String.join("\n", List.of(
                "Ты — карьерный консультант и планировщик обучения. Тебе дан граф навыков, на котором стрелками показана связь навыков в программировании. ",
                "Также данные ниже: матрица навыков пользователя, требования роли, ориентированный граф зависимостей навыков, список вакансий роли и список учебных ресурсов.",
                "Задача: выдать краткий маршрут, который быстрее всего закроет дефицит навыков для целевой роли; более сильные рёбра графа ставь раньше в очереди. Ты должен учитывать, что например для aws и kafka (это ты поймешь по графу) сначала надо изучить kubernetes, тогда даже если этот навык не требуется в специальности ты должен его вписать в шаги по достижению необходимого уровня. Поэтому например в случае с Java Backend Developer не пропускай этот шаг и аналогично для остальных случаев.",
            "Помни что в присланном тебе графе наибольшим приоритетом пользуются навыки вверху, то есть ты должен как бы идти сверху вниз.",
            "Отдавай приоритет книгам и курсам вне Stepik/Coursera, в первую очередь материалам похожим на Harvard/CS50.",
                "Для каждого шага выбери 1–2 ресурса из предложенного списка, которые лучше всего подходят под темы шага, и укажи их явно.",
            "\nОриентированный граф навыков (используй уровни навыков для приоритета шагов, те у которых уровень меньше должны быть первее):\n" + graphJson,
            "\nТекущие навыки пользователя (1 = владеет):\n" + formatJson(userMatrix)
                        + "\nСильные стороны: " + String.join(", ", safeMastered),
                "\nМатрица требуемых навыков роли:\n" + formatJson(desiredMatrix)
                        + "\nКлючевые цели: " + String.join(", ", safeRequired),
                "\nНавыки, которых не хватает: " + (missingSkills.isEmpty() ? "нет" : String.join(", ", missingSkills)),
                "\nВакансии для анализа стеков и узких тем (используй для выбора фреймворков и инструментов):\n" + vacanciesJson,
                "\nСписок ресурсов: используй только их, подбирая к каждому шагу 1–2 варианта:\n" + RESOURCES_FOR_RECOMMENDATIONS,
                "\nФормат ответа:",
                "Roadmap: 3–7 шагов. Каждый шаг — одно предложение вида 'Шаг 1. Java: ООП, коллекции \n Ресурсы: Ресурсы: Learning Python — Марк Лутц \n → Шаг 2. Алгоритмы: асимптотика, разбиение.' или 'Шаг 2. Алгоритмы на Java: асимптотический анализ, толстое и тонкое разбиение. \n  *Ресурсы*",
                "Правила: сначала закрывай пробелы в навыках, опираясь на уже освоенные; сильные рёбра графа = более важные зависимости, ставь их раньше; разрешено указывать фреймворки, темы, технологии; не повторяй уже освоенные навыки как отдельные шаги — только используй их как фундамент; для каждого шага перечисли 1–2 подходящих ресурса из списка выше. Не пытайся писать жирным шрифтом. Не пиши ничего после предложенных шагов по улучшению"
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
