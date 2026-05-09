package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves LLM-generated item names to catalog {@link Equipment} via three
 * progressively more lenient strategies:
 * <ol>
 *   <li>Exact string match</li>
 *   <li>Case-insensitive match</li>
 *   <li>Jaro-Winkler similarity ≥ 0.85</li>
 * </ol>
 *
 * <p>The 0.85 threshold is tight enough to catch obvious typos
 * ("Arduino Uno" → "Arduino Uno R3", "esp32 dev" → "ESP32 dev board")
 * without conflating different items ("DHT22" vs "BME280"). Items with
 * no acceptable match keep {@code equipment == null} and will be dropped
 * by {@link HallucinationFilter} downstream.
 */
@Component
@Order(10)
@Slf4j
public class NameResolver implements RecommendationValidator {

    /** Items below this Jaro-Winkler score are considered hallucinations, not typos. */
    static final double SIMILARITY_THRESHOLD = 0.85;

    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    @Override
    public List<ValidatedItem> validate(List<ValidatedItem> items, List<Equipment> catalog) {
        return items.stream()
                .map(item -> resolve(item, catalog))
                .toList();
    }

    private ValidatedItem resolve(ValidatedItem item, List<Equipment> catalog) {
        if (item.equipment() != null) {
            return item;
        }

        String name = item.originalLlmName();

        // 1. Exact match — fast path, the common case for well-behaved LLMs
        for (Equipment eq : catalog) {
            if (eq.getName().equals(name)) {
                return item.withEquipment(eq);
            }
        }

        // 2. Case-insensitive match
        for (Equipment eq : catalog) {
            if (eq.getName().equalsIgnoreCase(name)) {
                log.debug("Resolved '{}' to '{}' via case-insensitive match", name, eq.getName());
                return item.withEquipment(eq);
            }
        }

        // 3. Fuzzy match (Jaro-Winkler)
        Equipment bestMatch = null;
        double bestScore = 0.0;
        String nameLower = name.toLowerCase();
        for (Equipment eq : catalog) {
            double score = similarity.apply(nameLower, eq.getName().toLowerCase());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = eq;
            }
        }
        if (bestMatch != null && bestScore >= SIMILARITY_THRESHOLD) {
            log.debug("Resolved '{}' to '{}' via fuzzy match (score={})",
                    name, bestMatch.getName(), bestScore);
            return item.withEquipment(bestMatch);
        }

        log.debug("No catalog match for LLM name '{}' (best fuzzy score: {})", name, bestScore);
        return item;
    }
}