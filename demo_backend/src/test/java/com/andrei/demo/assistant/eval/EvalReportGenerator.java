package com.andrei.demo.assistant.eval;

import com.andrei.demo.assistant.validator.AvailabilityStatus;
import com.andrei.demo.assistant.validator.ValidatedItem;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Renders an {@link EvalReport} as Markdown.
 *
 * <p>Pure function: same input produces the same output, modulo
 * {@code generatedAt}. Sections that don't apply (e.g. availability
 * breakdown when no validators ran) are simply omitted.
 */
public class EvalReportGenerator {

    public String render(EvalReport report) {
        StringBuilder sb = new StringBuilder();
        renderHeader(sb, report);
        renderSummary(sb, report);
        renderAvailabilityIfPresent(sb, report);
        renderPerCaseTable(sb, report);
        renderDetailedErrors(sb, report);
        renderMethodology(sb);
        return sb.toString();
    }

    private void renderHeader(StringBuilder sb, EvalReport r) {
        sb.append("# Project Assistant — Evaluation Report\n\n");
        sb.append("- **Generated:** ").append(
                DateTimeFormatter.ISO_INSTANT.format(r.generatedAt())).append("\n");
        sb.append("- **Prompt version:** `").append(r.promptVersion()).append("`\n");
        sb.append("- **Model:** `").append(r.model()).append("`\n");
        sb.append("- **Catalog size:** ").append(r.catalogSize()).append(" items\n");
        sb.append("- **Golden set size:** ").append(r.totalCases()).append(" cases\n\n");
    }

    private void renderSummary(StringBuilder sb, EvalReport r) {
        sb.append("## Summary metrics\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|---|---|\n");
        sb.append(String.format(Locale.ROOT, "| Mean precision | **%.3f** |%n", r.meanPrecision()));
        sb.append(String.format(Locale.ROOT, "| Mean recall | **%.3f** |%n", r.meanRecall()));
        sb.append(String.format(Locale.ROOT, "| Mean F1 | **%.3f** |%n", r.meanF1()));
        sb.append(String.format(Locale.ROOT, "| Parse success rate | %d / %d (%.1f%%) |%n",
                r.parseSuccesses(), r.totalCases(),
                100.0 * r.parseSuccesses() / Math.max(1, r.totalCases())));
        sb.append(String.format(Locale.ROOT, "| Total items returned | %d |%n", r.totalReturnedItems()));
        sb.append(String.format(Locale.ROOT, "| Hallucinations | %d |%n", r.totalHallucinations()));
        sb.append(String.format(Locale.ROOT, "| Hallucination rate | %.2f%% |%n", 100.0 * r.hallucinationRate()));
        sb.append(String.format(Locale.ROOT, "| Latency p50 | %d ms |%n", r.latencyP50Ms()));
        sb.append(String.format(Locale.ROOT, "| Latency p95 | %d ms |%n", r.latencyP95Ms()));
        sb.append(String.format(Locale.ROOT, "| Latency max | %d ms |%n", r.latencyMaxMs()));
        sb.append(String.format(Locale.ROOT, "| Total prompt tokens | %d |%n", r.totalPromptTokens()));
        sb.append(String.format(Locale.ROOT, "| Total completion tokens | %d |%n", r.totalCompletionTokens()));
        sb.append("\n");
    }

    private void renderAvailabilityIfPresent(StringBuilder sb, EvalReport r) {
        // Skip the section entirely if the validator chain didn't run.
        if (r.itemsAvailable() + r.itemsInsufficientStock() + r.itemsOutOfStock() == 0) {
            return;
        }
        int total = r.itemsAvailable() + r.itemsInsufficientStock()
                + r.itemsOutOfStock() + r.itemsUnknownAvailability();
        sb.append("## Stock availability (validator chain output)\n\n");
        sb.append("| Status | Count | Share |\n");
        sb.append("|---|---|---|\n");
        sb.append(String.format(Locale.ROOT, "| ✓ AVAILABLE | %d | %.1f%% |%n",
                r.itemsAvailable(), pct(r.itemsAvailable(), total)));
        sb.append(String.format(Locale.ROOT, "| ⚠ INSUFFICIENT_STOCK | %d | %.1f%% |%n",
                r.itemsInsufficientStock(), pct(r.itemsInsufficientStock(), total)));
        sb.append(String.format(Locale.ROOT, "| ✗ OUT_OF_STOCK | %d | %.1f%% |%n",
                r.itemsOutOfStock(), pct(r.itemsOutOfStock(), total)));
        if (r.itemsUnknownAvailability() > 0) {
            sb.append(String.format(Locale.ROOT, "| — UNKNOWN | %d | %.1f%% |%n",
                    r.itemsUnknownAvailability(), pct(r.itemsUnknownAvailability(), total)));
        }
        sb.append("\n");
    }

    private double pct(int part, int total) {
        return total == 0 ? 0.0 : 100.0 * part / total;
    }

    private void renderPerCaseTable(StringBuilder sb, EvalReport r) {
        sb.append("## Per-case results\n\n");
        sb.append("| Case | Parsed | P | R | F1 | Hall. | Latency | Recommended |\n");
        sb.append("|---|---|---|---|---|---|---|---|\n");
        for (EvalCaseResult c : r.caseResults()) {
            String items = c.returnedItems().stream()
                    .map(this::renderItem)
                    .collect(Collectors.joining(", "));
            sb.append(String.format(Locale.ROOT,
                    "| `%s` | %s | %.2f | %.2f | %.2f | %d | %d ms | %s |%n",
                    c.caseId(),
                    c.parseSucceeded() ? "✓" : "✗",
                    c.precision(), c.recall(), c.f1(),
                    c.hallucinations().size(),
                    c.latencyMs(),
                    items.isEmpty() ? "(none)" : items));
        }
        sb.append("\n");
    }

    private String renderItem(ValidatedItem item) {
        String base = item.effectiveName() + " ×" + item.quantity();
        String symbol = availabilitySymbol(item.availabilityStatus());
        return symbol.isEmpty() ? base : base + " " + symbol;
    }

    private String availabilitySymbol(AvailabilityStatus s) {
        if (s == null) return "";
        return switch (s) {
            case AVAILABLE -> "✓";
            case INSUFFICIENT_STOCK -> "⚠";
            case OUT_OF_STOCK -> "✗";
        };
    }

    private void renderDetailedErrors(StringBuilder sb, EvalReport r) {
        List<EvalCaseResult> failures = r.caseResults().stream()
                .filter(c -> !c.parseSucceeded()
                        || !c.falseNegatives().isEmpty()
                        || !c.falsePositives().isEmpty()
                        || !c.hallucinations().isEmpty())
                .toList();
        if (failures.isEmpty()) {
            sb.append("## Detailed errors\n\n")
                    .append("None — every case scored a clean P/R/F1 = 1.0 with zero hallucinations.\n\n");
            return;
        }
        sb.append("## Detailed errors\n\n");
        for (EvalCaseResult c : failures) {
            sb.append("### `").append(c.caseId()).append("`\n\n");
            if (!c.parseSucceeded()) {
                sb.append("- **Parse failed:** ").append(c.parseError()).append("\n\n");
                continue;
            }
            if (!c.falseNegatives().isEmpty()) {
                sb.append("- **Missed (false negatives):** ").append(c.falseNegatives()).append("\n");
            }
            if (!c.falsePositives().isEmpty()) {
                sb.append("- **Unjustified (false positives):** ").append(c.falsePositives()).append("\n");
            }
            if (!c.hallucinations().isEmpty()) {
                sb.append("- **Hallucinated (not in catalog):** ").append(c.hallucinations()).append("\n");
            }
            sb.append("\n");
        }
    }

    private void renderMethodology(StringBuilder sb) {
        sb.append("## Methodology\n\n");
        sb.append("- **Precision** = TP / (TP + FP). FP = returned items that were neither expected nor flagged as acceptable.\n");
        sb.append("- **Recall** = TP / |expected|. Missing an expected item costs recall.\n");
        sb.append("- **F1** = harmonic mean of precision and recall.\n");
        sb.append("- **Hallucinations** = returned items whose name does not match any catalog entry. Hallucinations are a strict subset of false positives.\n");
        sb.append("- **Latency** is end-to-end time including the LLM call (prompt assembly and parsing are negligible by comparison).\n");
        sb.append("- **Tokens** are taken from the provider's `usage` block. Calls that don't report usage contribute 0.\n");
        sb.append("- **Availability** is only annotated when the validator chain runs. ✓ = AVAILABLE, ⚠ = INSUFFICIENT_STOCK, ✗ = OUT_OF_STOCK.\n");
        sb.append("- Each case has an `expectedItems` set (must appear) and an optional `acceptableItems` set (OK to appear, doesn't penalise precision).\n");
        sb.append("- Cases where parsing fails contribute precision/recall/F1 = 0 to the means.\n");
    }
}