package com.aicareer.aitransform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public final class SkillsExtraction {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DEFAULT_MODEL_PATH = System.getenv().getOrDefault(
            "OPENAI_MODEL",
            "gpt-4o-mini"
    );

    private static final String SKILLS_RESOURCE = "skills.json";

    private static final List<String> SKILL_LIST = loadSkillList();

    private SkillsExtraction() {
    }

    public static Map<String, Integer> fromFile(Path path) {
        try {
            return fromJson(Files.readString(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read vacancies file: " + path, e);
        }
    }

    public static List<String> skillList() {
        return List.copyOf(SKILL_LIST);
    }

    public static Map<String, Integer> fromResource(String resource) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + resource);
            return fromJson(new String(is.readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load vacancies resource: " + resource, e);
        }
    }

    public static Map<String, Integer> fromJson(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            if (!root.isArray()) {
                throw new IllegalArgumentException("Vacancies payload must be a JSON array");
            }
            return requestFromModel(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON provided for vacancies", e);
        }
    }

    private static Map<String, Integer> requestFromModel(String vacanciesJson) {
        String prompt = ExtractionPrompt.build()
                + "\n\nVacancies JSON (analyze them together and return only the skills matrix):\n"
                + vacanciesJson
                + "\n\nReturn only the JSON object with the skill flags.";
        System.out.println("[AI] Строим матрицу навыков через модель OpenAI...");
        String rawResponse = new OpenAIClient()
                .generate(DEFAULT_MODEL_PATH, prompt);
        System.out.println("[AI] Ответ по навыкам получен, разбираем...");

        String jsonResponse = extractJson(rawResponse);
        try {
            JsonNode matrixNode = MAPPER.readTree(jsonResponse);
            Map<String, Integer> matrix = new LinkedHashMap<>();
            for (String skill : SKILL_LIST) {
                int value = readInt(matrixNode, skill);
                matrix.put(skill, value == 0 ? 0 : 1);
            }
            return matrix;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse model response as skill matrix", e);
        }
    }

    private static int readInt(JsonNode node, String field) {
        if (!node.has(field)) {
            return 0;
        }
        JsonNode valueNode = node.get(field);
        if (valueNode.isInt() || valueNode.isLong()) {
            return valueNode.asInt();
        }
        if (valueNode.isTextual()) {
            try {
                return Integer.parseInt(valueNode.asText().trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String extractJson(String text) {
        try {
            MAPPER.readTree(text);
            return text;
        } catch (IOException ignored) {
        }

        int fenceStart = text.indexOf("```json");
        if (fenceStart >= 0) {
            int afterFence = text.indexOf('`', fenceStart + 7);
            int fenceEnd = text.indexOf("```", afterFence + 1);
            if (afterFence >= 0 && fenceEnd > afterFence) {
                String body = text.substring(afterFence + 1, fenceEnd);
                return body.trim();
            }
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) {
            throw new IllegalStateException("Model response does not contain a JSON object");
        }
        return text.substring(start, end + 1);
    }

    private static List<String> loadSkillList() {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(SKILLS_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Skills resource not found: " + SKILLS_RESOURCE);
            }
            return MAPPER.readValue(is, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load skills list from resource: " + SKILLS_RESOURCE, e);
        }
    }

    public static String resolveVacanciesResource(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Название роли не может быть пустым");
        }

        String safe = roleName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String resource = "export/vacancies_top25_" + safe + ".json";

        if (resourceExists(resource)) {
            return resource;
        }

        throw new IllegalArgumentException("Файл вакансий top25 не найден для роли: " + roleName);
    }

    private static boolean resourceExists(String resource) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResource(resource) != null;
    }

    public static void main(String[] args) {
        if (args.length == 0 || args[0].isBlank()) {
            throw new IllegalArgumentException("Укажите название роли или путь к JSON с вакансиями");
        }

        Path providedPath = Path.of(args[0]);
        String source = Files.exists(providedPath)
                ? providedPath.toString()
                : resolveVacanciesResource(args[0]);

        Map<String, Integer> matrix = source.startsWith("export/")
                ? fromResource(source)
                : fromFile(Path.of(source));
        try {
            System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(matrix));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize matrix", e);
        }
    }

}
