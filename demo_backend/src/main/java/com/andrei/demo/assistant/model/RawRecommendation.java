package com.andrei.demo.assistant.model;

import java.util.List;

/**
 * Strict, validated representation of the LLM's top-level response.
 *
 * <p>"Raw" because it has not yet been through the validator chain
 * (Phase 3) — names may not match the catalog, quantities may exceed
 * stock, the same item could appear twice, etc. The validator chain
 * cleans all that up.
 */
public record RawRecommendation(
        String projectPlan,
        List<RawRecommendedItem> items
) {
}