package com.andrei.demo.assistant.model;

/**
 * Lifecycle of a {@link RecommendationSession}:
 * <ul>
 *   <li>{@code DRAFT} — freshly recommended, awaiting student submit.</li>
 *   <li>{@code SUBMITTED} — student converted it to a loan request;
 *       {@code loanRecord} is set.</li>
 *   <li>{@code EXPIRED} — session aged out without submission. Reserved
 *       for a Phase 6 cleanup job; no code path sets this yet.</li>
 * </ul>
 */
public enum RecommendationSessionStatus {
    DRAFT,
    SUBMITTED,
    EXPIRED
}