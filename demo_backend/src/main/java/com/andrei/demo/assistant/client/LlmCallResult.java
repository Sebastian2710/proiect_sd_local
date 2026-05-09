package com.andrei.demo.assistant.client;

/**
 * Rich result from a single LLM call: the assistant content plus
 * observability metadata (latency and token usage). Used by the eval
 * suite (Phase 2) and the audit log (Phase 6); regular callers use
 * {@link LlmClient#complete} which returns just the content.
 *
 * <p>{@code promptTokens} and {@code completionTokens} are nullable
 * because not every provider reports them. Gemini, OpenAI, Groq, and
 * OpenRouter all return a {@code usage} block; some local proxies don't.
 */
public record LlmCallResult(
        String content,
        long latencyMs,
        Integer promptTokens,
        Integer completionTokens
) {
    public Integer totalTokens() {
        if (promptTokens == null || completionTokens == null) {
            return null;
        }
        return promptTokens + completionTokens;
    }
}