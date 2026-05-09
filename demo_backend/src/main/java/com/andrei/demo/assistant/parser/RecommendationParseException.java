package com.andrei.demo.assistant.parser;

/**
 * Thrown when the LLM response cannot be parsed into a valid
 * {@link com.andrei.demo.assistant.model.RawRecommendation}.
 *
 * <p>Every parse failure is a measurable signal in the eval suite,
 * so the message should clearly identify what was wrong (which field,
 * what was expected, what was received).
 */
public class RecommendationParseException extends RuntimeException {

    public RecommendationParseException(String message) {
        super(message);
    }

    public RecommendationParseException(String message, Throwable cause) {
        super(message, cause);
    }
}