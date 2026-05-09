package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines multiple {@link ValidatedItem} entries that point to the same
 * {@link Equipment}. Quantities are summed; reasons are concatenated
 * (deduplicated, semicolon-separated).
 *
 * <p>Runs after {@link NameResolver} so two LLM strings that resolve to
 * the same catalog entry (e.g. "Arduino Uno" and "Arduino Uno R3") are
 * treated as one. The {@link LinkedHashMap} preserves first-seen order
 * so the merged list reflects the LLM's original priority.
 */
@Component
@Order(30)
@Slf4j
public class DuplicateMerger implements RecommendationValidator {

    @Override
    public List<ValidatedItem> validate(List<ValidatedItem> items, List<Equipment> catalog) {
        Map<String, List<ValidatedItem>> groups = new LinkedHashMap<>();
        for (ValidatedItem item : items) {
            String key = item.equipment() == null
                    ? "__unmatched_" + System.identityHashCode(item)
                    : item.equipment().getName();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        List<ValidatedItem> merged = new ArrayList<>(groups.size());
        int mergedRefs = 0;
        for (List<ValidatedItem> group : groups.values()) {
            if (group.size() == 1) {
                merged.add(group.get(0));
            } else {
                merged.add(merge(group));
                mergedRefs += group.size() - 1;
            }
        }
        if (mergedRefs > 0) {
            log.info("Merged {} duplicate item reference(s)", mergedRefs);
        }
        return merged;
    }

    private ValidatedItem merge(List<ValidatedItem> dupes) {
        ValidatedItem first = dupes.get(0);
        int totalQty = dupes.stream().mapToInt(ValidatedItem::quantity).sum();
        String mergedReason = dupes.stream()
                .map(ValidatedItem::reason)
                .distinct()
                .reduce((a, b) -> a + "; " + b)
                .orElse(first.reason());
        return new ValidatedItem(
                first.originalLlmName(),
                first.equipment(),
                totalQty,
                mergedReason,
                first.availabilityStatus()
        );
    }
}