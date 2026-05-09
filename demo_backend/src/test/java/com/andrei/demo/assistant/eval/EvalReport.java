package com.andrei.demo.assistant.eval;

import java.time.Instant;
import java.util.List;

/**
 * Aggregate result across the whole golden set.
 *
 * <p>The four {@code items*} fields at the bottom track availability status
 * across all returned items. They are non-zero only when the validator chain
 * ran (Phase 3); for the Phase 2 baseline run, {@code itemsUnknownAvailability}
 * equals the total count and the others are zero.
 */
public record EvalReport(
        Instant generatedAt,
        String promptVersion,
        String model,
        int catalogSize,
        int totalCases,
        int parseSuccesses,
        int parseFailures,
        double meanPrecision,
        double meanRecall,
        double meanF1,
        int totalReturnedItems,
        int totalHallucinations,
        double hallucinationRate,
        long latencyP50Ms,
        long latencyP95Ms,
        long latencyMaxMs,
        long totalPromptTokens,
        long totalCompletionTokens,
        int itemsAvailable,
        int itemsInsufficientStock,
        int itemsOutOfStock,
        int itemsUnknownAvailability,
        List<EvalCaseResult> caseResults
) {
}