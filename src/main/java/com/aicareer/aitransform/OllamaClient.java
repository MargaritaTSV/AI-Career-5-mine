package com.aicareer.aitransform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
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
        this.endpoint = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
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
