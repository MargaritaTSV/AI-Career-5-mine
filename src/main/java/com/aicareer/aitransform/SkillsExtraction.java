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

public final class SkillsExtraction {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DEFAULT_MODEL_PATH = "deepseek-r1:8b";
    private static final String DEFAULT_OLLAMA_HOST =
            System.getenv().getOrDefault("OLLAMA_HOST", "http://localhost:11434");

    private static final String SKILLS_RESOURCE = "skills.json";

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
        return loadSkillList();
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
        List<String> skills = skillList();

        String prompt = ExtractionPrompt.build(skills)
                + "\n\nVacancies JSON (analyze them together and return only the skills matrix):\n"
                + vacanciesJson
                + "\n\nReturn only the JSON object with the skill flags.";

        String rawResponse = new OllamaClient(DEFAULT_OLLAMA_HOST)
                .generate(DEFAULT_MODEL_PATH, prompt);

        String jsonResponse = extractJson(rawResponse);
        try {
            JsonNode matrixNode = MAPPER.readTree(jsonResponse);
            Map<String, Integer> matrix = new LinkedHashMap<>();
            for (String skill : skills) {
                int value = matrixNode.has(skill) && matrixNode.get(skill).isNumber()
                        ? matrixNode.get(skill).asInt()
                        : 0;
                matrix.put(skill, value == 0 ? 0 : 1);
            }
            return matrix;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse model response as skill matrix", e);
        }
    }

    private static String extractJson(String text) {
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

    public static void main(String[] args) {
        String defaultPath = "src/main/resources/samples/skills-extraction-sample.json";
        String pathString;
        if (args.length == 0) {
            System.err.println("No arguments provided, using default sample file:");
            System.err.println("  " + defaultPath);
            pathString = defaultPath;
        } else if (args.length == 1) {
            pathString = args[0];
        } else {
            System.err.println("Usage: SkillsExtraction <path-to-vacancies-json>");
            System.exit(1);
            return;
        }

        Path path = Path.of(pathString);
        Map<String, Integer> matrix = fromFile(path);
        try {
            System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(matrix));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize matrix", e);
        }
    }

}
