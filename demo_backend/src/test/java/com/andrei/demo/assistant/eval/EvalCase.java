package com.andrei.demo.assistant.eval;

import java.util.List;

/**
 * One entry in the evaluation golden set. Loaded from
 * {@code src/test/java/resources/eval/golden_set.json}.
 *
 * <p>{@code expectedItems} = catalog names that MUST appear in the
 * recommendation. Missing one of these costs recall.
 *
 * <p>{@code acceptableItems} = catalog names that MAY appear without
 * penalising precision (e.g. a breadboard that's reasonable but not
 * strictly required). Distinct from expectedItems so we don't over-
 * specify what "correct" means and unfairly punish reasonable variation.
 *
 * <p>{@code rationale} is human-readable commentary, ignored by the
 * evaluator. Useful when reviewing the report.
 */
public record EvalCase(
        String id,
        String description,
        List<String> expectedItems,
        List<String> acceptableItems,
        String rationale
) {
    public List<String> expectedItemsOrEmpty() {
        return expectedItems == null ? List.of() : expectedItems;
    }

    public List<String> acceptableItemsOrEmpty() {
        return acceptableItems == null ? List.of() : acceptableItems;
    }
}