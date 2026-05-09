package com.andrei.demo.assistant.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * {@link LlmClient} adapter for any provider that speaks OpenAI's
 * {@code /chat/completions} HTTP protocol.
 *
 * <p>{@link #completeRich} is the only interface method this class implements;
 * {@link #complete} is satisfied by the default in {@link LlmClient}.
 */
@Component
@Slf4j
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final double TEMPERATURE = 0.2;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Duration timeout;

    public OpenAiCompatibleLlmClient(
            ObjectMapper objectMapper,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.model}") String model,
            @Value("${llm.timeout-seconds}") int timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public LlmCallResult completeRich(String systemPrompt, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new LlmException("systemPrompt must not be null or blank");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new LlmException("userPrompt must not be null or blank");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", TEMPERATURE,
                "response_format", Map.of("type", "json_object")
        );

        long start = System.currentTimeMillis();
        log.info("Calling LLM model={}, systemPromptChars={}, userPromptChars={}",
                model, systemPrompt.length(), userPrompt.length());

        String rawEnvelope;
        try {
            rawEnvelope = webClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(timeout);
        } catch (WebClientResponseException e) {
            log.error("LLM provider returned HTTP {} : {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LlmException(
                    "LLM provider returned HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RuntimeException e) {
            log.error("LLM call failed: {}", e.getMessage());
            throw new LlmException("LLM call failed: " + e.getMessage(), e);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("LLM call completed in {}ms", elapsed);

        if (rawEnvelope == null || rawEnvelope.isBlank()) {
            throw new LlmException("LLM provider returned empty response body");
        }

        return parseEnvelope(rawEnvelope, elapsed);
    }

    private LlmCallResult parseEnvelope(String envelope, long latencyMs) {
        try {
            JsonNode root = objectMapper.readTree(envelope);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new LlmException("LLM response missing 'choices' array: " + envelope);
            }
            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isMissingNode() || !content.isTextual()) {
                throw new LlmException("LLM response missing choices[0].message.content: " + envelope);
            }
            String text = content.asText();
            if (text.isBlank()) {
                throw new LlmException("LLM response content is blank");
            }

            Integer promptTokens = readOptionalInt(root, "usage", "prompt_tokens");
            Integer completionTokens = readOptionalInt(root, "usage", "completion_tokens");

            return new LlmCallResult(text, latencyMs, promptTokens, completionTokens);
        } catch (LlmException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LlmException("Failed to parse LLM envelope: " + e.getMessage(), e);
        }
    }

    private Integer readOptionalInt(JsonNode root, String... path) {
        JsonNode node = root;
        for (String p : path) {
            node = node.path(p);
        }
        return (node.isInt() || node.isLong()) ? node.asInt() : null;
    }
}