package com.andrei.demo.assistant.model;

/**
 * One recommended item as returned by the LLM, before validation.
 *
 * <p>{@code name} is whatever the model wrote — usually a verbatim
 * catalog name, but could also be a slight typo, a synonym, or
 * (despite the prompt instructions) a fully hallucinated item.
 * The Phase 3 validator chain reconciles this against the catalog.
 */
public record RawRecommendedItem(
        String name,
        int quantity,
        String reason
) {
}