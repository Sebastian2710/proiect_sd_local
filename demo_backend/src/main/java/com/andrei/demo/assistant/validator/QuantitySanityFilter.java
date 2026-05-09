package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Catches LLM mistakes around quantities. Items with non-positive quantity
 * are dropped; items with absurdly high quantity (above
 * {@link #MAX_REASONABLE_QUANTITY}) are clamped, on the assumption the
 * intent was right but the number was hallucinated.
 *
 * <p>Stock-aware capping is intentionally NOT done here — that's
 * {@link StockAvailabilityAnnotator}'s job, and it's an annotation, not a
 * filter, because the user may want to see "you asked for 5 but only 3 are
 * available" instead of having the value silently changed.
 */
@Component
@Order(40)
@Slf4j
public class QuantitySanityFilter implements RecommendationValidator {

    /**
     * Absolute upper bound, regardless of stock. The catalog has unit-scale
     * items (boards, packs, rings) where any individual student project rarely
     * needs more than a handful. Anything above this is almost certainly an
     * LLM mistake (e.g. quantity=100). 20 is a generous ceiling.
     */
    static final int MAX_REASONABLE_QUANTITY = 20;

    @Override
    public List<ValidatedItem> validate(List<ValidatedItem> items, List<Equipment> catalog) {
        return items.stream()
                .filter(this::isPositive)
                .map(this::clampIfAbsurd)
                .toList();
    }

    private boolean isPositive(ValidatedItem item) {
        if (item.quantity() < 1) {
            log.warn("Dropping item '{}' with non-positive quantity {}",
                    item.effectiveName(), item.quantity());
            return false;
        }
        return true;
    }

    private ValidatedItem clampIfAbsurd(ValidatedItem item) {
        if (item.quantity() > MAX_REASONABLE_QUANTITY) {
            log.warn("Clamping quantity of '{}' from {} to {}",
                    item.effectiveName(), item.quantity(), MAX_REASONABLE_QUANTITY);
            return item.withQuantity(MAX_REASONABLE_QUANTITY);
        }
        return item;
    }
}