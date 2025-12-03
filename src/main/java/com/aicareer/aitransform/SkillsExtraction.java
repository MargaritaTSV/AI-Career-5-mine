package com.aicareer.aitransform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SkillsExtraction {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DEFAULT_MODEL_PATH = "deepseek-r1:8b";
    private static final String DEFAULT_OLLAMA_HOST =
            System.getenv().getOrDefault("OLLAMA_HOST", "http://localhost:11434");

    private static final String SKILLS_RESOURCE = "skills.json";

    private static final List<String> SKILL_LIST = loadSkillList();

    // Разрешить ли локальный fallback по окружению. По умолчанию false — если Ollama недоступна, приложение завершится с ошибкой.
    private static final boolean ALLOW_LOCAL_FALLBACK = Boolean.parseBoolean(
            System.getenv().getOrDefault("ALLOW_LOCAL_FALLBACK", "false")
    );

    // Флаг, указывающий, была ли использована локальная стратегия (fallback)
    private static volatile boolean usedFallback = false;

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
        // Сбрасываем флаг перед попыткой LLM
        usedFallback = false;

        String prompt = ExtractionPrompt.build()
                + "\n\nVacancies JSON (analyze them together and return only the skills matrix):\n"
                + vacanciesJson
                + "\n\nReturn only the JSON object with the skill flags.";

        String rawResponse = null;
        try {
            rawResponse = new OllamaClient(DEFAULT_OLLAMA_HOST)
                    .generate(DEFAULT_MODEL_PATH, prompt);
        } catch (RuntimeException e) {
            // Ищем причину подключения в цепочке исключений
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof ConnectException || cause instanceof ClosedChannelException) {
                    String rootMsg = getRootCauseMessage(e);
                    String msg = "Cannot connect to Ollama at " + DEFAULT_OLLAMA_HOST + ". Root cause: " + rootMsg;
                    if (!ALLOW_LOCAL_FALLBACK) {
                        // Не разрешено делать локальный fallback — кидаем фатальную ошибку
                        throw new IllegalStateException(msg + ". To allow local fallback set ALLOW_LOCAL_FALLBACK=true", e);
                    }
                    System.err.println("[WARN] " + msg + ". Falling back to local extraction (simple keyword search). To disable fallback, unset ALLOW_LOCAL_FALLBACK or set it to false.");
                    usedFallback = true;
                    return localExtractSkills(vacanciesJson);
                }
                cause = cause.getCause();
            }
            // Если причина не связана с подключением — либо фатал, либо fallback по флагу
            String rootMsg = getRootCauseMessage(e);
            String msg = "Ollama call failed: " + rootMsg;
            if (!ALLOW_LOCAL_FALLBACK) {
                throw new IllegalStateException(msg + ". To allow local fallback set ALLOW_LOCAL_FALLBACK=true", e);
            }
            System.err.println("[WARN] " + msg + ". Falling back to local extraction.");
            usedFallback = true;
            return localExtractSkills(vacanciesJson);
        }

        if (rawResponse == null) {
            // Невероятный случай — считаем fallback либо фатальной ошибкой
            String msg = "Ollama returned null response";
            if (!ALLOW_LOCAL_FALLBACK) {
                throw new IllegalStateException(msg + ". To allow local fallback set ALLOW_LOCAL_FALLBACK=true");
            }
            System.err.println("[WARN] " + msg + "; using local fallback.");
            usedFallback = true;
            return localExtractSkills(vacanciesJson);
        }

        String jsonResponse = extractJson(rawResponse);
        try {
            JsonNode matrixNode = MAPPER.readTree(jsonResponse);
            Map<String, Integer> matrix = new LinkedHashMap<>();
            for (String skill : SKILL_LIST) {
                int value = matrixNode.has(skill) && matrixNode.get(skill).isNumber()
                        ? matrixNode.get(skill).asInt()
                        : 0;
                matrix.put(skill, value == 0 ? 0 : 1);
            }
            // Успешно получили ответ от модели — usedFallback остаётся false
            return matrix;
        } catch (JsonProcessingException e) {
            String msg = "Failed to parse model response as JSON skill matrix: " + e.getMessage();
            if (!ALLOW_LOCAL_FALLBACK) {
                throw new IllegalStateException(msg + ". To allow local fallback set ALLOW_LOCAL_FALLBACK=true", e);
            }
            System.err.println("[WARN] " + msg + ". Falling back to local extraction.");
            usedFallback = true;
            return localExtractSkills(vacanciesJson);
        }
    }

    private static String getRootCauseMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) root = root.getCause();
        return root.getClass().getSimpleName() + ": " + (root.getMessage() == null ? "(no message)" : root.getMessage());
    }

    /**
     * Простая локальная стратегия извлечения навыков: поиск термов навыков в тексте входного JSON.
     * Это fallback, который обеспечивает предсказуемый выход, если модель недоступна.
     */
    private static Map<String, Integer> localExtractSkills(String vacanciesJson) {
        usedFallback = true;
        String lower = vacanciesJson.toLowerCase(Locale.ROOT);
        Map<String, Integer> matrix = new LinkedHashMap<>();
        for (String skill : SKILL_LIST) {
            String key = skill.toLowerCase(Locale.ROOT);
            boolean found = lower.contains(key);
            matrix.put(skill, found ? 1 : 0);
        }
        return matrix;
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

        // Ясное сообщение о том, какой метод извлечения использовался
        if (usedFallback) {
            System.err.println("[INFO] Extraction performed by LOCAL fallback (no Ollama). Use OLLAMA_HOST env var or start Ollama to enable LLM-based extraction.");
        } else {
            System.err.println("[INFO] Extraction performed by LLM via Ollama (model: " + DEFAULT_MODEL_PATH + ", host: " + DEFAULT_OLLAMA_HOST + ")");
        }

        try {
            System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(matrix));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize matrix", e);
        }
    }

}
