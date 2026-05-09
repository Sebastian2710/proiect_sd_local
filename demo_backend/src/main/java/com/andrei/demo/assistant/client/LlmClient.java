package com.andrei.demo.assistant.client;

/**
 * Adapter boundary between the orchestrator and any concrete LLM provider.
 *
 * <p>Implementations expose a single primitive operation, {@link #completeRich},
 * that returns the assistant's content along with observability metadata
 * (latency, token usage). The convenience {@link #complete} method extracts
 * just the content for callers that don't care about metadata.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *   <li>Authenticating to the provider</li>
 *   <li>Sending the system + user prompts as one completion request</li>
 *   <li>Returning the raw assistant content (typically JSON, but parsing/
 *       validation is the caller's responsibility)</li>
 *   <li>Translating transport-layer failures into {@link LlmException}</li>
 * </ul>
 */
public interface LlmClient {

    /**
     * Send a completion request and return the rich call result —
     * content plus latency and token usage.
     *
     * @param systemPrompt the system message; may not be null or blank
     * @param userPrompt   the user message; may not be null or blank
     * @return the call result; never null
     * @throws LlmException if the request fails, times out, or the response is malformed
     */
    LlmCallResult completeRich(String systemPrompt, String userPrompt);

    /**
     * Convenience: like {@link #completeRich} but discards latency / token metadata.
     */
    default String complete(String systemPrompt, String userPrompt) {
        return completeRich(systemPrompt, userPrompt).content();
    }
}