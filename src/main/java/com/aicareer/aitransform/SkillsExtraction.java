package com.aicareer.aitransform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkillsExtraction {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DEFAULT_MODEL_PATH = System.getenv().getOrDefault(
            "OPENAI_MODEL",
            "gpt-4o-mini"
    );

    private static final List<String> SKILL_LIST = List.of(
            "java",
            "c++",
            "python",
            "javascript",
            "sql",
            "docker",
            "c#",
            "php",
            "spring",
            "machine_learning",
            "react",
            "typescript",
            "kubernetes",
            "terraform",
            "linux",
            "hibernate",
            "spark",
            "distributed_systems",
            "kafka",
            "aws"
    );

    private SkillsExtraction() {
    }

    public static List<String> skillList() {
        return List.copyOf(SKILL_LIST);
    }

    public static Map<String, Integer> fromVacancies(List<?> vacancies) {
        try {
            return fromJson(MAPPER.writeValueAsString(vacancies));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize vacancies payload", e);
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

    public static void main(String[] args) {
        throw new UnsupportedOperationException("Use the interactive pipeline to extract skills");
    }

}
