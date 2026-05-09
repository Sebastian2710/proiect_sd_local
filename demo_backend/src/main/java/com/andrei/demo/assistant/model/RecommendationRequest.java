package com.andrei.demo.assistant.model;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body of {@code POST /assistant/recommend}. The student's project description
 * plus the date they plan to return the borrowed equipment.
 */
public record RecommendationRequest(
        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 5000, message = "Description must be 10–5000 characters")
        String description,

        @NotNull(message = "Expected return date is required")
        @Future(message = "Expected return date must be in the future")
        LocalDate expectedReturnDate
) {
}