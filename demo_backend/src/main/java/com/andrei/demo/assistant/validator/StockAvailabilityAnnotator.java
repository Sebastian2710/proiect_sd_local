package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Annotates each {@link ValidatedItem} with an {@link AvailabilityStatus}
 * based on its requested quantity vs the catalog stock count. Pure
 * annotator — does not drop, clamp, or otherwise change the item.
 *
 * <p>Runs last in the chain because it depends on quantities being already
 * sanity-checked by {@link QuantitySanityFilter}.
 */
@Component
@Order(50)
@Slf4j
public class StockAvailabilityAnnotator implements RecommendationValidator {

    @Override
    public List<ValidatedItem> validate(List<ValidatedItem> items, List<Equipment> catalog) {
        return items.stream()
                .map(this::annotate)
                .toList();
    }

    private ValidatedItem annotate(ValidatedItem item) {
        if (item.equipment() == null) {
            // Should never happen — HallucinationFilter strips these.
            // Defensive: leave untouched rather than NPE.
            return item;
        }
        int stock = item.equipment().getStockCount() == null
                ? 0 : item.equipment().getStockCount();
        AvailabilityStatus status;
        if (stock == 0) {
            status = AvailabilityStatus.OUT_OF_STOCK;
        } else if (item.quantity() > stock) {
            status = AvailabilityStatus.INSUFFICIENT_STOCK;
        } else {
            status = AvailabilityStatus.AVAILABLE;
        }
        return item.withAvailabilityStatus(status);
    }
}