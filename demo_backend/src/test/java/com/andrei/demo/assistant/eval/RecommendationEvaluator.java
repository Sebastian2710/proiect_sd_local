package com.andrei.demo.assistant.eval;

import com.andrei.demo.assistant.client.LlmCallResult;
import com.andrei.demo.assistant.client.OpenAiCompatibleLlmClient;
import com.andrei.demo.assistant.model.RawRecommendation;
import com.andrei.demo.assistant.model.RawRecommendedItem;
import com.andrei.demo.assistant.parser.RecommendationParser;
import com.andrei.demo.assistant.prompt.PromptBuilder;
import com.andrei.demo.assistant.validator.AvailabilityStatus;
import com.andrei.demo.assistant.validator.RecommendationValidator;
import com.andrei.demo.assistant.validator.ValidatedItem;
import com.andrei.demo.model.Equipment;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs the full Project Assistant pipeline against a {@link EvalCase}
 * golden set and computes per-case + aggregate metrics.
 *
 * <p>Two operational modes:
 * <ul>
 *   <li><b>Phase 2 baseline:</b> empty {@code validators} list. Items are
 *       scored on the LLM's raw output names.</li>
 *   <li><b>Phase 3 validated:</b> the full validator chain is applied
 *       between parsing and scoring. Items are scored on canonical
 *       (resolved) catalog names; availability is annotated.</li>
 * </ul>
 *
 * <p>{@code minCallGapMs} adds a minimum elapsed-time gap between successive
 * LLM calls — useful to stay under free-tier rate limits (e.g. 12000 keeps
 * Gemini Flash's 5 RPM happy).
 */
@Slf4j
public class RecommendationEvaluator {

    private final OpenAiCompatibleLlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final RecommendationParser parser;
    private final List<Equipment> catalog;
    private final Set<String> catalogNames;
    private final List<RecommendationValidator> validators;
    private final long minCallGapMs;

    private long lastCallStartMs = 0L;

    /** Phase 2 constructor: no validators, no rate-limit pacing. */
    public RecommendationEvaluator(
            OpenAiCompatibleLlmClient llmClient,
            PromptBuilder promptBuilder,
            RecommendationParser parser,
            List<Equipment> catalog
    ) {
        this(llmClient, promptBuilder, parser, catalog, List.of(), 0L);
    }

    /** Phase 3 constructor: full validator chain + configurable inter-call pacing. */
    public RecommendationEvaluator(
            OpenAiCompatibleLlmClient llmClient,
            PromptBuilder promptBuilder,
            RecommendationParser parser,
            List<Equipment> catalog,
            List<RecommendationValidator> validators,
            long minCallGapMs
    ) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.catalog = catalog;
        this.catalogNames = catalog.stream()
                .map(Equipment::getName)
                .collect(Collectors.toUnmodifiableSet());
        this.validators = validators == null ? List.of() : validators;
        this.minCallGapMs = Math.max(0L, minCallGapMs);
    }

    /** Run the full eval suite and return the aggregate report. */
    public EvalReport run(List<EvalCase> cases, String model) {
        List<EvalCaseResult> caseResults = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            EvalCase c = cases.get(i);
            log.info("Evaluating case {}/{}: {}", i + 1, cases.size(), c.id());
            caseResults.add(evaluateOne(c));
        }
        return aggregate(caseResults, model);
    }

    private EvalCaseResult evaluateOne(EvalCase c) {
        PromptBuilder.Prompt prompt = promptBuilder.build(
                c.description(), catalog, LocalDate.now().plusWeeks(2));

        waitForRateLimit();

        LlmCallResult call;
        try {
            call = llmClient.completeRich(prompt.system(), prompt.user());
        } catch (RuntimeException e) {
            log.warn("LLM call failed for case {}: {}", c.id(), e.getMessage());
            return failedResult(c.id(), 0L, null, null, "LLM call failed: " + e.getMessage());
        }

        RawRecommendation rec;
        try {
            rec = parser.parse(call.content());
        } catch (RuntimeException e) {
            log.warn("Parse failed for case {}: {}", c.id(), e.getMessage());
            return failedResult(
                    c.id(), call.latencyMs(),
                    call.promptTokens(), call.completionTokens(),
                    "Parse failed: " + e.getMessage());
        }

        List<ValidatedItem> items = applyValidatorChain(rec.items());
        return scoreOne(c, items, call);
    }

    /** Convert raw parser output into {@link ValidatedItem}s and run the chain. */
    private List<ValidatedItem> applyValidatorChain(List<RawRecommendedItem> rawItems) {
        List<ValidatedItem> items = rawItems.stream()
                .map(r -> new ValidatedItem(r.name(), null, r.quantity(), r.reason(), null))
                .collect(Collectors.toList());
        for (RecommendationValidator v : validators) {
            items = v.validate(items, catalog);
        }
        return items;
    }

    private void waitForRateLimit() {
        if (minCallGapMs <= 0) {
            return;
        }
        long elapsed = System.currentTimeMillis() - lastCallStartMs;
        if (lastCallStartMs > 0 && elapsed < minCallGapMs) {
            long sleep = minCallGapMs - elapsed;
            log.debug("Pacing: sleeping {}ms to respect rate limit", sleep);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallStartMs = System.currentTimeMillis();
    }

    private EvalCaseResult scoreOne(EvalCase c, List<ValidatedItem> items, LlmCallResult call) {
        Set<String> expected = new HashSet<>(c.expectedItemsOrEmpty());
        Set<String> acceptable = new HashSet<>(c.acceptableItemsOrEmpty());

        Set<String> returnedNames = items.stream()
                .map(ValidatedItem::effectiveName)
                .collect(Collectors.toCollection(HashSet::new));

        // Hallucination = name not present in the catalog at all.
        // After Phase 3's HallucinationFilter this set is empty by construction;
        // for Phase 2 baseline it reflects the LLM's raw output.
        Set<String> hallucinations = returnedNames.stream()
                .filter(n -> !catalogNames.contains(n))
                .collect(Collectors.toCollection(HashSet::new));

        Set<String> tp = intersect(returnedNames, expected);
        Set<String> fp = returnedNames.stream()
                .filter(n -> !expected.contains(n))
                .filter(n -> !acceptable.contains(n))
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> fn = expected.stream()
                .filter(n -> !returnedNames.contains(n))
                .collect(Collectors.toCollection(HashSet::new));

        double precision = (tp.size() + fp.size()) == 0
                ? 1.0
                : (double) tp.size() / (tp.size() + fp.size());
        double recall = expected.isEmpty()
                ? 1.0
                : (double) tp.size() / expected.size();
        double f1 = (precision + recall) == 0.0
                ? 0.0
                : 2 * precision * recall / (precision + recall);

        return new EvalCaseResult(
                c.id(), true, null,
                call.latencyMs(),
                call.promptTokens(), call.completionTokens(),
                items,
                tp, fp, fn, hallucinations,
                precision, recall, f1
        );
    }

    private EvalCaseResult failedResult(String id, long latencyMs,
                                        Integer pt, Integer ct, String error) {
        return new EvalCaseResult(
                id, false, error, latencyMs, pt, ct,
                List.of(),
                Set.of(), Set.of(), Set.of(), Set.of(),
                0.0, 0.0, 0.0
        );
    }

    private EvalReport aggregate(List<EvalCaseResult> caseResults, String model) {
        int total = caseResults.size();
        int succeeded = (int) caseResults.stream().filter(EvalCaseResult::parseSucceeded).count();
        int failed = total - succeeded;

        List<EvalCaseResult> ok = caseResults.stream()
                .filter(EvalCaseResult::parseSucceeded)
                .toList();

        double meanP = mean(ok.stream().mapToDouble(EvalCaseResult::precision).toArray());
        double meanR = mean(ok.stream().mapToDouble(EvalCaseResult::recall).toArray());
        double meanF1 = mean(ok.stream().mapToDouble(EvalCaseResult::f1).toArray());

        int totalReturned = ok.stream().mapToInt(r -> r.returnedItems().size()).sum();
        int totalHall = ok.stream().mapToInt(r -> r.hallucinations().size()).sum();
        double hallRate = totalReturned == 0 ? 0.0 : (double) totalHall / totalReturned;

        long[] latencies = caseResults.stream().mapToLong(EvalCaseResult::latencyMs).sorted().toArray();
        long p50 = percentile(latencies, 0.50);
        long p95 = percentile(latencies, 0.95);
        long max = latencies.length == 0 ? 0L : latencies[latencies.length - 1];

        long totalPt = caseResults.stream()
                .mapToLong(r -> r.promptTokens() == null ? 0 : r.promptTokens()).sum();
        long totalCt = caseResults.stream()
                .mapToLong(r -> r.completionTokens() == null ? 0 : r.completionTokens()).sum();

        // Availability counts (only meaningful when the chain ran).
        int avail = 0, insufficient = 0, outOfStock = 0, unknown = 0;
        for (EvalCaseResult r : ok) {
            for (ValidatedItem item : r.returnedItems()) {
                AvailabilityStatus s = item.availabilityStatus();
                if (s == null) unknown++;
                else if (s == AvailabilityStatus.AVAILABLE) avail++;
                else if (s == AvailabilityStatus.INSUFFICIENT_STOCK) insufficient++;
                else if (s == AvailabilityStatus.OUT_OF_STOCK) outOfStock++;
            }
        }

        return new EvalReport(
                Instant.now(),
                PromptBuilder.PROMPT_VERSION,
                model,
                catalog.size(),
                total, succeeded, failed,
                meanP, meanR, meanF1,
                totalReturned, totalHall, hallRate,
                p50, p95, max,
                totalPt, totalCt,
                avail, insufficient, outOfStock, unknown,
                Collections.unmodifiableList(caseResults)
        );
    }

    private static Set<String> intersect(Set<String> a, Set<String> b) {
        Set<String> out = new HashSet<>(a);
        out.retainAll(b);
        return out;
    }

    private static double mean(double[] xs) {
        if (xs.length == 0) return 0.0;
        double sum = 0;
        for (double x : xs) sum += x;
        return sum / xs.length;
    }

    private static long percentile(long[] sorted, double p) {
        if (sorted.length == 0) return 0L;
        int idx = (int) Math.ceil(p * sorted.length) - 1;
        if (idx < 0) idx = 0;
        if (idx >= sorted.length) idx = sorted.length - 1;
        return sorted[idx];
    }
}