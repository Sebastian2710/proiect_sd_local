package com.andrei.demo.assistant.eval;

import com.andrei.demo.assistant.validator.ValidatedItem;

import java.util.List;
import java.util.Set;

/**
 * Result of evaluating a single {@link EvalCase}.
 *
 * <p>{@code returnedItems} are {@link ValidatedItem}s — for the Phase 2
 * baseline run, each item has {@code equipment == null} and
 * {@code availabilityStatus == null} (no validators ran). For Phase 3,
 * both fields are populated.
 */
public record EvalCaseResult(
        String caseId,
        boolean parseSucceeded,
        String parseError,                  // non-null when parseSucceeded == false
        long latencyMs,
        Integer promptTokens,
        Integer completionTokens,
        List<ValidatedItem> returnedItems,
        Set<String> truePositives,          // returned ∩ expected
        Set<String> falsePositives,         // returned but neither expected nor acceptable
        Set<String> falseNegatives,         // expected but not returned
        Set<String> hallucinations,         // returned but not in catalog at all
        double precision,                   // TP / (TP + FP); 1.0 if TP+FP == 0
        double recall,                      // TP / |E|; 1.0 if |E| == 0
        double f1
) {
}