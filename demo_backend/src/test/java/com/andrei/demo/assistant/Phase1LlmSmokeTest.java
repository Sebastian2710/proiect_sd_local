package com.andrei.demo.assistant;

import com.andrei.demo.assistant.client.OpenAiCompatibleLlmClient;
import com.andrei.demo.assistant.model.RawRecommendation;
import com.andrei.demo.assistant.model.RawRecommendedItem;
import com.andrei.demo.assistant.parser.RecommendationParser;
import com.andrei.demo.assistant.prompt.PromptBuilder;
import com.andrei.demo.model.Equipment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end smoke test against the configured real LLM provider
 * (Google Gemini by default, via its OpenAI-compatible endpoint).
 *
 * <p>Skipped automatically when {@code LLM_API_KEY} is not set, so CI
 * remains green without secrets and developers without a key can still
 * run the rest of the suite.
 *
 * <p>The test does NOT mock anything. It hits the real network, the
 * real model, and the real (free-tier) quota. Run sparingly. The point
 * is to verify Phase 1 wiring works end-to-end; deeper accuracy testing
 * is Phase 2's job.
 *
 * <p>To run locally with the default Gemini configuration:
 * <pre>
 *   export LLM_API_KEY=&lt;your Google AI Studio key&gt;
 *   ./mvnw test -Dtest=Phase1LlmSmokeTest
 * </pre>
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@EnabledIfEnvironmentVariable(
        named = "LLM_API_KEY",
        matches = ".+",
        disabledReason = "Requires real LLM_API_KEY environment variable"
)
class Phase1LlmSmokeTest {

    @Autowired
    private OpenAiCompatibleLlmClient llmClient;

    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private RecommendationParser parser;

    @Test
    void endToEnd_realLlm_returnsParsableRecommendation() {
        // -- given --------------------------------------------------------
        List<Equipment> catalog = sampleCatalog();
        String description = """
                I want to build a portable weather station that records
                temperature, humidity and barometric pressure to an SD card
                every minute, runs on battery for at least a week, and uploads
                a daily summary to a server over WiFi when it's in range.
                """;
        LocalDate expectedReturn = LocalDate.now().plusWeeks(3);

        // -- when ---------------------------------------------------------
        PromptBuilder.Prompt prompt = promptBuilder.build(description, catalog, expectedReturn);

        long start = System.currentTimeMillis();
        String raw = llmClient.complete(prompt.system(), prompt.user());
        long elapsedMs = System.currentTimeMillis() - start;

        RawRecommendation rec = parser.parse(raw);

        // -- then ---------------------------------------------------------
        assertNotNull(rec, "parsed recommendation must not be null");
        assertNotNull(rec.projectPlan(), "projectPlan must not be null");
        assertFalse(rec.projectPlan().isBlank(), "projectPlan must not be blank");
        assertNotNull(rec.items(), "items must not be null");
        assertTrue(rec.items().size() <= catalog.size(),
                "should not recommend more items than the catalog has");

        // -- print full result for human inspection -----------------------
        System.out.println();
        System.out.println("================ Phase 1 smoke test ================");
        System.out.println("Latency       : " + elapsedMs + " ms");
        System.out.println("Prompt version: " + PromptBuilder.PROMPT_VERSION);
        System.out.println("Catalog size  : " + catalog.size());
        System.out.println("Description   : " + description.strip().replaceAll("\\s+", " "));
        System.out.println();
        System.out.println("---- raw LLM response (first 500 chars) ----");
        System.out.println(raw.length() > 500 ? raw.substring(0, 500) + "…" : raw);
        System.out.println();
        System.out.println("---- parsed projectPlan ----");
        System.out.println(rec.projectPlan());
        System.out.println();
        System.out.println("---- parsed items (" + rec.items().size() + ") ----");
        for (RawRecommendedItem item : rec.items()) {
            System.out.printf("  - %-30s  qty=%d  reason=%s%n",
                    item.name(), item.quantity(), item.reason());
        }
        System.out.println("=====================================================");
        System.out.println();
    }

    /**
     * A small, hand-crafted catalog roughly matching what a real lending
     * lab might stock. Exact-name copying behaviour is one of the things
     * the test verifies, so spellings here are intentionally non-trivial
     * (e.g. "BME280 sensor", "ESP32 dev board").
     */
    private List<Equipment> sampleCatalog() {
        return List.of(
                makeEquipment("ESP32 dev board",
                        "WiFi+BLE microcontroller, 5V via USB-C", 4),
                makeEquipment("Arduino Uno R3",
                        "Classic 8-bit microcontroller, 5V", 6),
                makeEquipment("BME280 sensor",
                        "Combined temperature, humidity, pressure I2C sensor", 8),
                makeEquipment("MicroSD breakout",
                        "SPI MicroSD card slot breakout board, 3.3V", 5),
                makeEquipment("18650 battery pack",
                        "2x 18650 cells, 7.4V, with built-in protection", 3)
        );
    }

    private Equipment makeEquipment(String name, String description, int stock) {
        Equipment eq = new Equipment();
        eq.setName(name);
        eq.setDescription(description);
        eq.setStockCount(stock);
        return eq;
    }
}