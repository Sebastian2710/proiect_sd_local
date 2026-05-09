package com.andrei.demo.assistant.parser;

import com.andrei.demo.assistant.model.RawRecommendation;
import com.andrei.demo.assistant.model.RawRecommendedItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Parses the LLM's raw response string into a {@link RawRecommendation}.
 *
 * <p>We deliberately do schema validation by walking {@link JsonNode} ourselves
 * rather than relying solely on Jackson's {@code FAIL_ON_UNKNOWN_PROPERTIES}.
 * Two reasons:
 * <ol>
 *   <li><b>Better error messages.</b> "items[2].quantity must be a positive
 *       integer, got null" tells us exactly what the model misbehaved on,
 *       which is gold for the eval suite — every parse failure is a metric
 *       on its own.</li>
 *   <li><b>Defence-in-depth.</b> Jackson auto-mapping of records is forgiving
 *       in subtle ways (e.g. accepting {@code "1"} as int 1 if a coercer is
 *       registered). Walking the tree lets us be strict on types.</li>
 * </ol>
 *
 * <p>The parser also tolerates the most common deviation we still see despite
 * the system-prompt instructions and {@code response_format=json_object}:
 * Markdown code fences wrapping the JSON. Anything stranger is an error.
 */
@Component
@Slf4j
public class RecommendationParser {

    private static final Set<String> ALLOWED_TOP_LEVEL_KEYS = Set.of("projectPlan", "items");
    private static final Set<String> ALLOWED_ITEM_KEYS = Set.of("name", "quantity", "reason");

    private final ObjectMapper objectMapper;

    public RecommendationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse the raw LLM response into a validated {@link RawRecommendation}.
     *
     * @param rawContent the raw response text from {@code LlmClient.complete()}
     * @return the parsed and schema-validated recommendation
     * @throws RecommendationParseException if the content is not valid JSON,
     *         is missing required keys, has unknown keys, has wrong types,
     *         or violates field-level constraints
     */
    public RawRecommendation parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new RecommendationParseException("LLM response is null or blank");
        }

        String stripped = stripCodeFences(rawContent.strip());
        log.debug("Parsing recommendation, {} chars after stripping", stripped.length());

        JsonNode root;
        try {
            root = objectMapper.readTree(stripped);
        } catch (JacksonException e) {
            throw new RecommendationParseException(
                    "Response is not valid JSON: " + e.getOriginalMessage(), e);
        }

        if (root == null || !root.isObject()) {
            throw new RecommendationParseException(
                    "Top-level JSON value must be an object, got: "
                            + (root == null ? "null" : root.getNodeType()));
        }

        rejectUnknownTopLevelKeys(root);

        String projectPlan = readRequiredString(root, "projectPlan", "projectPlan");
        JsonNode itemsNode = root.path("items");
        if (itemsNode.isMissingNode()) {
            throw new RecommendationParseException("Missing required field: items");
        }
        if (!itemsNode.isArray()) {
            throw new RecommendationParseException(
                    "'items' must be an array, got: " + itemsNode.getNodeType());
        }

        List<RawRecommendedItem> items = new ArrayList<>();
        for (int i = 0; i < itemsNode.size(); i++) {
            items.add(parseItem(itemsNode.get(i), i));
        }

        return new RawRecommendation(projectPlan, items);
    }

    private RawRecommendedItem parseItem(JsonNode item, int index) {
        if (item == null || !item.isObject()) {
            throw new RecommendationParseException(
                    "items[" + index + "] must be an object, got: "
                            + (item == null ? "null" : item.getNodeType()));
        }
        rejectUnknownItemKeys(item, index);

        String name = readRequiredString(item, "name", "items[" + index + "].name");
        String reason = readRequiredString(item, "reason", "items[" + index + "].reason");

        JsonNode quantityNode = item.path("quantity");
        if (quantityNode.isMissingNode() || quantityNode.isNull()) {
            throw new RecommendationParseException(
                    "Missing required field: items[" + index + "].quantity");
        }
        if (!quantityNode.isInt() && !quantityNode.isLong() && !quantityNode.isShort()) {
            throw new RecommendationParseException(
                    "items[" + index + "].quantity must be an integer, got: "
                            + quantityNode.getNodeType());
        }
        int quantity = quantityNode.asInt();
        if (quantity < 1) {
            throw new RecommendationParseException(
                    "items[" + index + "].quantity must be >= 1, got: " + quantity);
        }

        return new RawRecommendedItem(name, quantity, reason);
    }

    private String readRequiredString(JsonNode parent, String key, String pathForError) {
        JsonNode node = parent.path(key);
        if (node.isMissingNode() || node.isNull()) {
            throw new RecommendationParseException("Missing required field: " + pathForError);
        }
        if (!node.isTextual()) {
            throw new RecommendationParseException(
                    pathForError + " must be a string, got: " + node.getNodeType());
        }
        String value = node.asText();
        if (value.isBlank()) {
            throw new RecommendationParseException(pathForError + " must not be blank");
        }
        return value;
    }

    private void rejectUnknownTopLevelKeys(JsonNode root) {
        Set<String> unknown = new HashSet<>();
        Iterator<String> names = root.propertyNames().iterator();
        while (names.hasNext()) {
            String n = names.next();
            if (!ALLOWED_TOP_LEVEL_KEYS.contains(n)) {
                unknown.add(n);
            }
        }
        if (!unknown.isEmpty()) {
            throw new RecommendationParseException(
                    "Unknown top-level keys: " + unknown + ". Allowed: " + ALLOWED_TOP_LEVEL_KEYS);
        }
    }

    private void rejectUnknownItemKeys(JsonNode item, int index) {
        Set<String> unknown = new HashSet<>();
        Iterator<String> names = item.propertyNames().iterator();
        while (names.hasNext()) {
            String n = names.next();
            if (!ALLOWED_ITEM_KEYS.contains(n)) {
                unknown.add(n);
            }
        }
        if (!unknown.isEmpty()) {
            throw new RecommendationParseException(
                    "Unknown keys in items[" + index + "]: " + unknown
                            + ". Allowed: " + ALLOWED_ITEM_KEYS);
        }
    }

    /**
     * Defensive: if the model ignores instructions and wraps its JSON in
     * Markdown fences ({@code ```json ... ```}), strip them. The
     * {@code response_format=json_object} setting makes this nearly impossible
     * for cloud providers, but other providers behind {@link
     * com.andrei.demo.assistant.client.LlmClient} may not enforce it.
     */
    private String stripCodeFences(String s) {
        if (!s.startsWith("```")) {
            return s;
        }
        int firstNewline = s.indexOf('\n');
        if (firstNewline < 0) {
            return s; // malformed but let the JSON parser fail naturally
        }
        String body = s.substring(firstNewline + 1);
        if (body.endsWith("```")) {
            body = body.substring(0, body.length() - 3);
        }
        return body.strip();
    }
}