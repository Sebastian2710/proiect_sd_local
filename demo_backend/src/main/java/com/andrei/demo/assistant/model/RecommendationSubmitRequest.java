package com.andrei.demo.assistant.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Body of {@code POST /assistant/recommend/{sessionId}/submit}. The student's
 * final selection, possibly with edited quantities or items removed.
 *
 * <p>Items are referenced by {@code recommendedItemId} (not {@code equipmentId})
 * so the server can verify that everything being submitted was part of the
 * recommendation — the user can't sneak in arbitrary equipment.
 */
public record RecommendationSubmitRequest(
        @NotEmpty(message = "Must submit at least one item")
        @Valid
        List<SubmitItem> items,

        /** Optional override; defaults to the session's expectedReturnDate. */
        @Future(message = "Expected return date must be in the future")
        LocalDate expectedReturnDate
) {

    public record SubmitItem(
            @NotNull(message = "recommendedItemId is required")
            UUID recommendedItemId,

            @Min(value = 1, message = "Quantity must be at least 1")
            int quantity
    ) {
    }
}