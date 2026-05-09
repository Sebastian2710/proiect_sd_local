package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drops every {@link ValidatedItem} that {@link NameResolver} could not
 * map to a catalog entry. Items reaching this filter with non-null
 * {@code equipment} pass through untouched.
 *
 * <p>This is the safety net: even if the LLM ignores the prompt's
 * grounding instructions, no fabricated item can reach the user-facing
 * recommendation.
 */
@Component
@Order(20)
@Slf4j
public class HallucinationFilter implements RecommendationValidator {

    @Override
    public List<ValidatedItem> validate(List<ValidatedItem> items, List<Equipment> catalog) {
        List<ValidatedItem> kept = items.stream()
                .filter(i -> i.equipment() != null)
                .toList();
        int dropped = items.size() - kept.size();
        if (dropped > 0) {
            log.info("Dropped {} hallucinated item(s) from recommendation", dropped);
            items.stream()
                    .filter(i -> i.equipment() == null)
                    .forEach(i -> log.debug("Dropped hallucinated item: '{}'", i.originalLlmName()));
        }
        return kept;
    }
}