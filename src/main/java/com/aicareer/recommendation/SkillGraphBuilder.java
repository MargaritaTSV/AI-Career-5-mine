package com.aicareer.recommendation;

import com.aicareer.aitransform.SkillsExtraction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class SkillGraphBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, Pattern> SKILL_PATTERNS = Map.of(
            "c++", Pattern.compile("\\bc\\s*\\+\\s*\\+\\b", Pattern.CASE_INSENSITIVE),
            "c#", Pattern.compile("\\bc\\s*#\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final Set<String> TEXT_FIELDS = Set.of(
            "title", "description", "snippet", "responsibilities",
            "requirements", "duties", "notes"
    );

    private SkillGraphBuilder() {
    }

    public static void main(String[] args) {
        Path baseDir = Path.of(args.length > 0 ? args[0] : "target/export");
        Path output = Path.of("target/graphs/skills-graph.json");

        SkillGraph graph = build(baseDir);
        writeGraph(output, graph);

        try {
            System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(graph));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to render graph as JSON", e);
        }
    }

    public static SkillGraph build(Path vacanciesDir) {
        List<String> skills = loadSkillList();
        Map<String, Integer> frequency = initFrequency(skills);

        List<Path> vacancyFiles = resolveVacancyFiles(vacanciesDir);
        if (vacancyFiles.isEmpty()) {
            throw new IllegalStateException("No vacancy files found in " + vacanciesDir.toAbsolutePath());
        }
        List<Set<String>> vacancySkills = new ArrayList<>();

        for (Path file : vacancyFiles) {
            JsonNode vacancies = readVacancies(file);
            for (JsonNode vacancy : vacancies) {
                Set<String> hits = detectSkills(vacancy, skills);
                vacancySkills.add(hits);
                hits.forEach(skill -> frequency.compute(skill, (k, v) -> v + 1));
            }
        }

        Map<EdgeKey, Integer> edges = buildEdges(vacancySkills, frequency);

        List<SkillNode> nodes = skills.stream()
                .map(skill -> new SkillNode(skill, frequency.getOrDefault(skill, 0)))
                .toList();

        List<SkillEdge> graphEdges = edges.entrySet()
                .stream()
                .sorted((a, b) -> {
                    int weightCompare = Integer.compare(b.getValue(), a.getValue());
                    if (weightCompare != 0) return weightCompare;
                    int fromCompare = a.getKey().from.compareTo(b.getKey().from);
                    if (fromCompare != 0) return fromCompare;
                    return a.getKey().to.compareTo(b.getKey().to);
                })
                .map(entry -> new SkillEdge(entry.getKey().from, entry.getKey().to, entry.getValue()))
                .toList();

        return new SkillGraph(nodes, graphEdges);
    }

    public static void writeGraph(Path output, SkillGraph graph) {
        try {
            Files.createDirectories(output.getParent());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), graph);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write graph to " + output, e);
        }
    }

    private static Map<EdgeKey, Integer> buildEdges(List<Set<String>> vacancySkills, Map<String, Integer> frequency) {
        Map<EdgeKey, Integer> edges = new HashMap<>();
        for (Set<String> skills : vacancySkills) {
            List<String> list = new ArrayList<>(skills);
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    String a = list.get(i);
                    String b = list.get(j);
                    String from = chooseBaseSkill(a, b, frequency);
                    String to = from.equals(a) ? b : a;
                    EdgeKey key = new EdgeKey(from, to);
                    edges.merge(key, 1, Integer::sum);
                }
            }
        }
        return edges;
    }

    private static String chooseBaseSkill(String a, String b, Map<String, Integer> frequency) {
        int freqA = frequency.getOrDefault(a, 0);
        int freqB = frequency.getOrDefault(b, 0);
        if (freqA > freqB) return a;
        if (freqB > freqA) return b;
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static Set<String> detectSkills(JsonNode vacancy, List<String> skills) {
        Set<String> hits = new LinkedHashSet<>();
        String text = collectText(vacancy);
        for (String skill : skills) {
            if (mentionsSkill(vacancy, text, skill)) {
                hits.add(skill);
            }
        }
        return hits;
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
        for (String field : TEXT_FIELDS) {
            JsonNode node = vacancy.get(field);
            if (node != null && node.isTextual()) {
                sb.append(' ').append(normalize(node.asText()));
            }
        }
        return sb.toString();
    }

    private static JsonNode readVacancies(Path file) {
        try {
            JsonNode root = MAPPER.readTree(Files.readString(file));
            if (!root.isArray()) {
                throw new IllegalArgumentException("Vacancies payload must be a JSON array: " + file);
            }
            return root;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read vacancies file: " + file, e);
        }
    }

    private static Map<String, Integer> initFrequency(List<String> skills) {
        Map<String, Integer> frequency = new LinkedHashMap<>();
        skills.forEach(skill -> frequency.put(skill, 0));
        return frequency;
    }

    private static List<Path> resolveVacancyFiles(Path vacanciesDir) {
        try (Stream<Path> stream = Files.list(vacanciesDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("vacancies_all_") && name.endsWith(".json");
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list vacancies in " + vacanciesDir, e);
        }
    }

    private static List<String> loadSkillList() {
        return SkillsExtraction.skillList()
                .stream()
                .map(SkillGraphBuilder::normalize)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String normalize(String text) {
        return Objects.requireNonNull(text, "text").toLowerCase(Locale.ROOT);
    }

    private record EdgeKey(String from, String to) {
        private EdgeKey {
            if (from.equals(to)) {
                throw new IllegalArgumentException("Edge cannot reference the same skill twice: " + from);
            }
        }
    }

    public record SkillGraph(List<SkillNode> nodes, List<SkillEdge> edges) {
    }

    public record SkillNode(String id, int vacancies) {
    }

    public record SkillEdge(String from, String to, int weight) {
    }
}
