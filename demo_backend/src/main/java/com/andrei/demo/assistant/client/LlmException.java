package com.andrei.demo.assistant.client;

/**
 * Wraps any failure that happens while talking to the LLM provider —
 * HTTP errors, timeouts, malformed envelopes, missing fields in the
 * response wrapper, etc.
 *
 * <p>Unchecked because callers (the orchestrator service) should treat
 * an LLM failure as a transient infrastructure problem and surface it
 * as a generic 5xx-style error to the user, not handle it locally.
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}