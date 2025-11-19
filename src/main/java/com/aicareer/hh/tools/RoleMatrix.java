package com.aicareer.hh.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class RoleMatrix {
    private RoleMatrix() {}

    public static Map<String, List<String>> load(String resourceName) {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + resourceName);

            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(is);
            Map<String, List<String>> out = new LinkedHashMap<>();

            // 1) Плоский словарь: { "Java Dev": ["java","spring"], ... }
            if (root.isObject() && !hasAny(root, "positions","roles","items")) {
                root.fields().forEachRemaining(e -> out.put(e.getKey(), readSkillsArray(e.getValue())));
                return requireNonEmpty(out, resourceName);
            }

            // 2) Объект с массивом: { "positions"/"roles"/"items": [ {role|title|name|position|profession, skills|stack|tags|keywords|hard_skills} ] }
            JsonNode arr = firstExisting(root, "positions","roles","items");
            if (arr != null && arr.isArray()) {
                for (JsonNode n : arr) acceptRoleNode(n, out);
                return requireNonEmpty(out, resourceName);
            }

            // 3) Верхний уровень — массив объектов ролей: [ { ... }, ... ]
            if (root.isArray()) {
                for (JsonNode n : root) acceptRoleNode(n, out);
                return requireNonEmpty(out, resourceName);
            }

            // 4) Фолбэк: попробуем как Map<String, List<String>>
            try (InputStream is2 = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(resourceName)) {
                Map<String, List<String>> m = om.readValue(is2, new TypeReference<>() {});
                return requireNonEmpty(m, resourceName);
            }
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void acceptRoleNode(JsonNode n, Map<String, List<String>> out) {
        if (!n.isObject()) return;
        String role = textFirst(n, "role","title","name","position","profession");
        JsonNode skillsNode = firstExisting(n, "skills","stack","tags","keywords","hard_skills","skills_req","skillsRequired");
        List<String> skills = readSkillsArray(skillsNode);
        if (role != null && !role.isBlank() && !skills.isEmpty()) {
            out.put(role, skills);
        }
    }

    private static Map<String, List<String>> requireNonEmpty(Map<String, List<String>> m, String src) {
        if (m == null || m.isEmpty()) throw new IllegalArgumentException("No roles parsed from " + src);
        return m;
    }

    private static boolean hasAny(JsonNode n, String... keys) {
        for (String k : keys) if (n.has(k)) return true;
        return false;
    }

    private static JsonNode firstExisting(JsonNode n, String... keys) {
        for (String k : keys) if (n.has(k)) return n.get(k);
        return null;
    }

    private static String textFirst(JsonNode n, String... keys) {
        for (String k : keys) if (n.has(k) && n.get(k).isTextual()) return n.get(k).asText();
        return null;
    }

    private static List<String> readSkillsArray(JsonNode node) {
        if (node == null) return List.of();
        if (node.isArray()) {
            return StreamSupport.stream(node.spliterator(), false)
                    .map(RoleMatrix::skillFromNode)
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (node.isTextual()) {
            return Arrays.stream(node.asText().split("[,;]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .distinct()
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private static String skillFromNode(JsonNode n) {
        if (n.isTextual()) return n.asText();
        for (String k : List.of("name","skill","title","value")) {
            if (n.has(k) && n.get(k).isTextual()) return n.get(k).asText();
        }
        return null;
    }
}
