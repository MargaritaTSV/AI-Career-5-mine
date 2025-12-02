package com.aicareer.aitransform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class SkillsExtraction {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> SKILL_LIST = List.of(
            "java", "c++", "python", "javascript", "sql",
            "docker", "c#", "php", "spring", "machine_learning"
    );

    private static final Map<String, Pattern> SKILL_PATTERNS = Map.of(
            "c++", Pattern.compile("\\bc\\s*\\+\\s*\\+\\b", Pattern.CASE_INSENSITIVE),
            "c#", Pattern.compile("\\bc\\s*#\\b", Pattern.CASE_INSENSITIVE)
    );

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
            return aggregate(root);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON provided for vacancies", e);
        }
    }

    private static Map<String, Integer> aggregate(JsonNode vacancies) {
        Map<String, Integer> hits = new LinkedHashMap<>();
        SKILL_LIST.forEach(skill -> hits.put(skill, 0));

        for (JsonNode vacancy : vacancies) {
            String text = collectText(vacancy);
            for (String skill : SKILL_LIST) {
                if (mentionsSkill(vacancy, text, skill)) {
                    hits.computeIfPresent(skill, (k, v) -> v + 1);
                }
            }
        }

        Map<String, Integer> matrix = new LinkedHashMap<>();
        for (String skill : SKILL_LIST) {
            matrix.put(skill, hits.getOrDefault(skill, 0) > 0 ? 1 : 0);
        }
        return matrix;
    }

    private static boolean mentionsSkill(JsonNode vacancy, String joinedText, String skill) {
        if (vacancy.has("skills") && vacancy.get("skills").isArray()) {
            for (JsonNode n : vacancy.get("skills")) {
                if (n.isTextual() && normalize(n.asText()).contains(skill)) {
                    return true;
                }
            }
        }

        Pattern pattern = SKILL_PATTERNS.getOrDefault(
                skill,
                Pattern.compile("\\b" + Pattern.quote(skill.replace('_', ' ')) + "\\b", Pattern.CASE_INSENSITIVE)
        );
        return pattern.matcher(joinedText).find();
    }

    private static String collectText(JsonNode vacancy) {
        StringBuilder sb = new StringBuilder();
        Set<String> fields = Set.of(
                "title", "description", "snippet", "responsibilities",
                "requirements", "duties", "notes"
        );
        for (String field : fields) {
            JsonNode node = vacancy.get(field);
            if (node != null && node.isTextual()) {
                sb.append(' ').append(normalize(node.asText()));
            }
        }
        return sb.toString();
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT);
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
