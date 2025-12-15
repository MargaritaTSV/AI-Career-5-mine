package com.aicareer.recommendation;

import com.aicareer.aitransform.OpenAIClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Invokes the OpenAI model with the generated roadmap prompt
 * and prints the model's roadmap response.
 */
public final class DeepseekRoadmapClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_MODEL_PATH = System.getenv().getOrDefault(
            "OPENAI_MODEL",
            "gpt-4o-mini"
    );
    private static final Path PROMPT_OUTPUT_PATH = Path.of("target", "roadmap-prompt.txt");

    private DeepseekRoadmapClient() {
    }

    public static void main(String[] args) {
        String prompt = RoadmapPromptBuilder.build();

        savePrompt(prompt);

        String response = executeInference(prompt, null);
        System.out.println(response);
    }

    public static void savePrompt(String prompt) {
        try {
            if (PROMPT_OUTPUT_PATH.getParent() != null) {
                Files.createDirectories(PROMPT_OUTPUT_PATH.getParent());
            }
            Files.writeString(PROMPT_OUTPUT_PATH, prompt);
//            System.err.println("Prompt saved to: " + PROMPT_OUTPUT_PATH.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save prompt to file", e);
        }
    }

    public static String generateRoadmap(String prompt) {
        return generateRoadmap(prompt, null);
    }

    public static String generateRoadmap(String prompt, Path skillGraphImage) {
        savePrompt(prompt);
        return executeInference(prompt, skillGraphImage);
    }

    private static String executeInference(String prompt, Path skillGraphImage) {
        try {
            String raw = new OpenAIClient()
                    .generate(DEFAULT_MODEL_PATH, prompt, skillGraphImage);
            return extractContent(raw);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke OpenAI model", e);
        }
    }

    private static String extractContent(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return responseBody;
            }
            JsonNode message = choices.get(0).path("message");
            JsonNode content = message.path("content");
            return content.isMissingNode() ? responseBody : content.asText();
        } catch (IOException e) {
            return responseBody;
        }
    }
}
