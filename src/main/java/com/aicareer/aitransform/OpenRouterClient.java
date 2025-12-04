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
import java.util.List;
import java.util.Map;

/**
 * Lightweight client for sending prompts to the OpenRouter chat completion API.
 */
public class OpenRouterClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    private static final String DEFAULT_MODEL = System.getenv().getOrDefault(
            "OPENROUTER_MODEL",
            "mistralai/mistral-7b-instruct:free"
    );
    private static final String DEFAULT_REFERER = System.getenv().getOrDefault(
            "OPENROUTER_REFERER",
            "https://example.com"
    );
    private static final String DEFAULT_TITLE = System.getenv().getOrDefault(
            "OPENROUTER_TITLE",
            "AI-Career-5"
    );

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String generate(String model, String prompt) {
        String apiKey = Config.API_KEY;

        String resolvedModel = (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", resolvedModel);
        payload.put("max_tokens", 2048);
        payload.put("messages", List.of(Map.of(
                "role", "user",
                "content", prompt
        )));

        try {
            String body = MAPPER.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", DEFAULT_REFERER)
                    .header("X-Title", DEFAULT_TITLE)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Model call failed: HTTP " + response.statusCode()
                        + " -> " + response.body());
            }

            JsonNode json = MAPPER.readTree(response.body());
            JsonNode choices = json.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode content = choices.get(0).path("message").path("content");
                if (!content.isMissingNode()) {
                    return content.asText();
                }
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Model call was interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call OpenRouter model", e);
        }
    }
}

