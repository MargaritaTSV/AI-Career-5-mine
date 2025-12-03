package com.aicareer.aitransform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class OllamaClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final URI endpoint;

    public OllamaClient(String baseUrl) {
        // Нормализуем URL и проверим, что подключение идёт только к локальным адресам, используемым при запуске через Docker.
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("OLLAMA host is not set. Ollama must be run via Docker and OLLAMA_HOST should point to a local address (e.g. http://localhost:11434).");
        }
        URI uri;
        try {
            uri = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid OLLAMA_HOST: " + baseUrl, ex);
        }

        String host = uri.getHost();
        int port = uri.getPort();
        // Разрешённые локальные хосты при запуске Ollama через Docker
        boolean allowedHost = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "0.0.0.0".equals(host)
                || "host.docker.internal".equalsIgnoreCase(host);

        // Разрешаем неуказанный порт или порт 11434 (стандартный для Ollama)
        boolean allowedPort = (port == -1) || (port == 11434);

        if (!allowedHost || !allowedPort) {
            String hint = "Ollama must run via Docker and be reachable on a local address. Set OLLAMA_HOST to e.g. http://localhost:11434 or http://host.docker.internal:11434";
            throw new IllegalArgumentException("Disallowed OLLAMA_HOST: " + baseUrl + ". " + hint);
        }

        this.endpoint = uri;
    }

    /**
     * Выбираем имя модели:
     * 1) OLLAMA_MODEL (если задан),
     * 2) modelPath из аргумента,
     * 3) дефолт "deepseek-r1:8b".
     */
    private static String resolveModelName(String modelPath) {
        String envModel = System.getenv("OLLAMA_MODEL");
        if (envModel != null && !envModel.isBlank()) {
            return envModel.trim();
        }
        if (modelPath != null && !modelPath.isBlank()) {
            return modelPath.trim();
        }
        return "deepseek-r1:8b"; // маленькая модель по умолчанию
    }

    public String generate(String modelPath, String prompt) {
        String model = resolveModelName(modelPath);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        payload.put("stream", false);

        try {
            String body = MAPPER.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(endpoint.resolve("api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Model call failed: HTTP " + response.statusCode() + " -> " + response.body());
            }

            JsonNode json = MAPPER.readTree(response.body());
            if (json.has("response")) {
                return json.get("response").asText();
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to call Ollama model", e);
        }
    }
}
