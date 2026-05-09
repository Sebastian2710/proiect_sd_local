package com.andrei.demo.assistant.validator;

/**
 * Stock-availability classification applied by {@link StockAvailabilityAnnotator}.
 * Stored on each {@link ValidatedItem} after the validator chain runs and
 * persisted on the corresponding {@code RecommendedItem} entity in Phase 4.
 */
public enum AvailabilityStatus {
    AVAILABLE,
    INSUFFICIENT_STOCK,
    OUT_OF_STOCK
}