package com.andrei.demo.assistant.prompt;

import com.andrei.demo.model.Equipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Builds the system + user prompt pair sent to the LLM.
 *
 * <p>The system prompt is a versioned deliverable: it is referenced verbatim
 * in the project documentation and run against the eval golden set in Phase 2.
 * Any change here must be reviewed because it directly affects the metrics
 * the TA is grading against.
 *
 * <p>Design choices baked into the prompt:
 * <ul>
 *   <li><b>Catalog grounding.</b> The full catalog is embedded as JSON in
 *       the user message; the system prompt forbids inventing items and
 *       requires verbatim name copying. This is the single biggest lever
 *       against hallucination.</li>
 *   <li><b>Strict JSON output.</b> Schema is documented in the system
 *       prompt, and {@code response_format=json_object} is set on the API
 *       call (see {@code OpenAiCompatibleLlmClient}) — belt-and-braces.</li>
 *   <li><b>Conservative quantities.</b> The model otherwise tends to over-
 *       order (3 sensors when 1 will do) which causes stock-availability
 *       failures downstream.</li>
 *   <li><b>De-duplication.</b> Asking for one entry per item up-front means
 *       the {@code DuplicateMerger} validator (Phase 3) almost never has
 *       work to do — but it's a safety net.</li>
 * </ul>
 */
@Component
@Slf4j
public class PromptBuilder {

    /**
     * Pair of (system, user) prompts ready to send to {@link
     * com.andrei.demo.assistant.client.LlmClient#complete}.
     */
    public record Prompt(String system, String user) {
    }

    /**
     * Versioned identifier for the prompt template. Bump this whenever
     * {@link #SYSTEM_PROMPT} changes so the eval report can attribute
     * metrics to a specific prompt version.
     */
    public static final String PROMPT_VERSION = "v1.0";

    /**
     * The production system prompt. KEEP IN SYNC WITH THE DOCS — or
     * rather, paste this block into the docs and don't edit there.
     */
    public static final String SYSTEM_PROMPT = """
            You are a project advisor for a university equipment-lending system.
            Students describe a project they want to build, and you help them by:
              1. Producing a clear, well-structured project plan in Markdown.
              2. Recommending specific items from the equipment catalog they can borrow.
            
            You must follow these rules without exception.
            
            ## GROUNDING
            - You MAY ONLY recommend items that appear in the EQUIPMENT CATALOG provided
              in the user message.
            - The "name" field in each recommendation MUST be copied verbatim from a
              catalog entry's "name" — same spelling, same casing, same punctuation.
            - If the project genuinely needs an item that is not in the catalog, do NOT
              invent or substitute one. Mention the gap in the project plan instead, so
              the student can source it themselves.
            
            ## QUANTITIES
            - Recommend the MINIMUM quantity the project realistically requires.
            - Default to 1 unless the project clearly needs more (e.g. an array of
              identical sensors, or one battery per device in a multi-node build).
            - Each catalog item appears AT MOST ONCE in the items array. Combine into
              a single entry with the appropriate quantity.
            - Quantity must be a positive integer.
            
            ## PROJECT PLAN
            - Markdown. 4–8 short paragraphs or bulleted sections.
            - Cover: build approach, key technical decisions, risks/considerations,
              and a suggested order of work.
            - Do NOT inline the equipment list inside the plan — that goes in the
              "items" array. The plan may reference items by name in passing.
            - If the catalog cannot fully support the project, say so explicitly in
              the plan.
            
            ## OUTPUT FORMAT
            - Reply with a SINGLE JSON object and nothing else. No prose, no preamble,
              no Markdown code fences, no commentary before or after.
            - The JSON MUST conform exactly to this schema:
            
            {
              "projectPlan": "<string, Markdown, required, non-empty>",
              "items": [
                {
                  "name":     "<string, exact catalog name, required>",
                  "quantity": <integer, >= 1, required>,
                  "reason":   "<string, one short sentence, required>"
                }
              ]
            }
            
            - "items" MAY be an empty array if nothing in the catalog fits the project,
              but the field must be present.
            - Do NOT include any keys other than those listed above.
            - Do NOT wrap the JSON in code fences.
            """;

    private static final String USER_TEMPLATE = """
            EQUIPMENT CATALOG (JSON):
            %s
            
            PROJECT DESCRIPTION:
            %s
            
            EXPECTED RETURN DATE: %s
            
            Produce the JSON object as specified.
            """;

    private final ObjectMapper objectMapper;

    public PromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Assemble the system + user prompt pair for a single recommendation.
     *
     * @param description        the student's free-text project description; required, non-blank
     * @param catalog            the full catalog of equipment available for loan; required
     * @param expectedReturnDate when the student expects to return the equipment; required
     * @return the assembled {@link Prompt}
     */
    public Prompt build(String description, List<Equipment> catalog, LocalDate expectedReturnDate) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be null or blank");
        }
        if (catalog == null) {
            throw new IllegalArgumentException("catalog must not be null");
        }
        if (expectedReturnDate == null) {
            throw new IllegalArgumentException("expectedReturnDate must not be null");
        }

        String catalogJson = serialiseCatalog(catalog);
        String userMessage = USER_TEMPLATE.formatted(
                catalogJson,
                description.strip(),
                expectedReturnDate
        );

        log.debug("Built prompt: catalogSize={}, descriptionChars={}",
                catalog.size(), description.length());

        return new Prompt(SYSTEM_PROMPT, userMessage);
    }

    /**
     * Serialize the catalog as a stable, minimal JSON array. We intentionally
     * project only the fields the model needs (name, description, stockCount).
     * Including JPA noise like {@code loanItems} would just bloat the prompt
     * and confuse the model.
     */
    private String serialiseCatalog(List<Equipment> catalog) {
        List<Map<String, Object>> projected = catalog.stream()
                .map(eq -> Map.<String, Object>of(
                        "name", eq.getName(),
                        "description", eq.getDescription() == null ? "" : eq.getDescription(),
                        "stockCount", eq.getStockCount() == null ? 0 : eq.getStockCount()
                ))
                .toList();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(projected);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialise equipment catalog for prompt", e);
        }
    }
}