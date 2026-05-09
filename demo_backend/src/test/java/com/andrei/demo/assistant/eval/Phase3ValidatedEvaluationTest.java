package com.andrei.demo.assistant.eval;

import com.andrei.demo.assistant.client.OpenAiCompatibleLlmClient;
import com.andrei.demo.assistant.parser.RecommendationParser;
import com.andrei.demo.assistant.prompt.PromptBuilder;
import com.andrei.demo.assistant.validator.RecommendationValidator;
import com.andrei.demo.model.Equipment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 3 evaluation runner — same golden set and catalog as Phase 2,
 * but with the full validator chain wired in between parsing and scoring.
 *
 * <p>Writes to {@code eval-report-validated.md} so the Phase 2 baseline
 * report ({@code eval-report.md}) is preserved for comparison.
 *
 * <p>Inter-call pacing of 12s keeps us under Gemini Flash's 5 RPM free-tier
 * limit. With 25 cases that's ~9 minutes wall time.
 *
 * <p>To run:
 * <pre>
 *   $env:LLM_API_KEY = "your key"
 *   ./mvnw test "-Dtest=Phase3ValidatedEvaluationTest"
 *   Get-Content eval-report-validated.md
 * </pre>
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@EnabledIfEnvironmentVariable(
        named = "LLM_API_KEY",
        matches = ".+",
        disabledReason = "Requires real LLM_API_KEY environment variable"
)
class Phase3ValidatedEvaluationTest {

    private static final long CALL_PACING_MS = 12_000L;
    private static final Path OUTPUT_PATH = Path.of("eval-report-validated.md");

    @Autowired private OpenAiCompatibleLlmClient llmClient;
    @Autowired private PromptBuilder promptBuilder;
    @Autowired private RecommendationParser parser;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private List<RecommendationValidator> validators;

    @Value("${llm.model}")
    private String model;

    @Test
    void runValidatedEvaluation_writesMarkdownReport() throws IOException {
        List<Equipment> catalog = loadCatalog();
        List<EvalCase> cases = loadGoldenSet();

        // Spring autowires List<RecommendationValidator> in @Order ascending,
        // so the chain runs NameResolver → Hallucination → Merger → Quantity → StockAnnotator.
        System.out.println("Validator chain order:");
        validators.forEach(v -> System.out.println("  - " + v.getClass().getSimpleName()));

        RecommendationEvaluator evaluator = new RecommendationEvaluator(
                llmClient, promptBuilder, parser, catalog, validators, CALL_PACING_MS);
        EvalReport report = evaluator.run(cases, model);

        String markdown = new EvalReportGenerator().render(report);
        Files.writeString(OUTPUT_PATH, markdown);

        System.out.println();
        System.out.println("Wrote validated eval report to " + OUTPUT_PATH.toAbsolutePath());
        System.out.println();
        System.out.println(markdown);

        assertNotNull(report);
        assertEquals(cases.size(), report.totalCases(),
                "every case should be represented in the report");
    }

    private List<Equipment> loadCatalog() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/eval/eval_catalog.json")) {
            if (is == null) {
                throw new IOException("eval_catalog.json not found on classpath");
            }
            return objectMapper.readValue(is, new TypeReference<List<Equipment>>() {});
        }
    }

    private List<EvalCase> loadGoldenSet() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/eval/golden_set.json")) {
            if (is == null) {
                throw new IOException("golden_set.json not found on classpath");
            }
            return objectMapper.readValue(is, new TypeReference<List<EvalCase>>() {});
        }
    }
}