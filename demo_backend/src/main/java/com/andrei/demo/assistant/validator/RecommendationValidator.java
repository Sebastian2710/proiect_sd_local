package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;

import java.util.List;

/**
 * One link in the validator chain (Chain of Responsibility pattern).
 *
 * <p>Each validator transforms a list of {@link ValidatedItem} into another
 * list — possibly shorter (filter), possibly with mutated items (annotator),
 * possibly with merged duplicates. Stateless and side-effect-free apart from
 * logging, so the chain is safe to invoke once per recommendation request.
 *
 * <p>Wired as an ordered {@code List<RecommendationValidator>} via Spring's
 * {@link org.springframework.core.annotation.Order} annotation. The
 * orchestrator iterates the list in @Order ascending. Current order:
 * <ol>
 *   <li>{@link NameResolver} (10) — resolve LLM names to catalog Equipment</li>
 *   <li>{@link HallucinationFilter} (20) — drop unresolved items</li>
 *   <li>{@link DuplicateMerger} (30) — combine items pointing to the same Equipment</li>
 *   <li>{@link QuantitySanityFilter} (40) — drop or clamp implausible quantities</li>
 *   <li>{@link StockAvailabilityAnnotator} (50) — set availability status</li>
 * </ol>
 */
public interface RecommendationValidator {
    List<ValidatedItem> validate(List<ValidatedItem> items, List<Equipment> catalog);
}