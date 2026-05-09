package com.andrei.demo.assistant.eval;

import com.andrei.demo.assistant.client.OpenAiCompatibleLlmClient;
import com.andrei.demo.assistant.parser.RecommendationParser;
import com.andrei.demo.assistant.prompt.PromptBuilder;
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
 * Phase 2 evaluation runner.
 *
 * <p>Loads {@code golden_set.json} and {@code eval_catalog.json} from
 * {@code src/test/java/resources/eval/}, runs the full pipeline against the
 * configured LLM provider, computes metrics, and writes
 * {@code eval-report.md} at the project root.
 *
 * <p>Skipped automatically when {@code LLM_API_KEY} is not set. With ~25
 * cases and ~10s latency per case on Gemini Flash free tier, expect
 * ~4-7 minutes of wall time.
 *
 * <p>To run:
 * <pre>
 *   export LLM_API_KEY=&lt;your key&gt;
 *   ./mvnw test -Dtest=Phase2EvaluationTest
 *   cat eval-report.md
 * </pre>
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@EnabledIfEnvironmentVariable(
        named = "LLM_API_KEY",
        matches = ".+",
        disabledReason = "Requires real LLM_API_KEY environment variable"
)
class Phase2EvaluationTest {

    @Autowired private OpenAiCompatibleLlmClient llmClient;
    @Autowired private PromptBuilder promptBuilder;
    @Autowired private RecommendationParser parser;
    @Autowired private ObjectMapper objectMapper;

    @Value("${llm.model}")
    private String model;

    @Test
    void runFullEvaluation_writesMarkdownReport() throws IOException {
        List<Equipment> catalog = loadCatalog();
        List<EvalCase> cases = loadGoldenSet();

        RecommendationEvaluator evaluator = new RecommendationEvaluator(
                llmClient, promptBuilder, parser, catalog);
        EvalReport report = evaluator.run(cases, model);

        String markdown = new EvalReportGenerator().render(report);
        Path outPath = Path.of("eval-report.md");
        Files.writeString(outPath, markdown);

        // Print to stdout for CI logs / quick inspection.
        System.out.println();
        System.out.println("Wrote eval report to " + outPath.toAbsolutePath());
        System.out.println();
        System.out.println(markdown);

        // Sanity assertions only — the metrics ARE the deliverable, so we
        // deliberately don't fail on a quality threshold here. If the suite
        // ran end-to-end and produced a report file, that's success.
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