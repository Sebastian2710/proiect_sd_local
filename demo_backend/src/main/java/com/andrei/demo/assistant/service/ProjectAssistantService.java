package com.andrei.demo.assistant.service;

import com.andrei.demo.assistant.client.LlmCallResult;
import com.andrei.demo.assistant.client.LlmClient;
import com.andrei.demo.assistant.model.RawRecommendation;
import com.andrei.demo.assistant.model.RawRecommendedItem;
import com.andrei.demo.assistant.model.RecommendationRequest;
import com.andrei.demo.assistant.model.RecommendationSession;
import com.andrei.demo.assistant.model.RecommendationSessionStatus;
import com.andrei.demo.assistant.model.RecommendationSubmitRequest;
import com.andrei.demo.assistant.model.RecommendedItem;
import com.andrei.demo.assistant.parser.RecommendationParser;
import com.andrei.demo.assistant.prompt.PromptBuilder;
import com.andrei.demo.assistant.repository.RecommendationSessionRepository;
import com.andrei.demo.assistant.validator.RecommendationValidator;
import com.andrei.demo.assistant.validator.ValidatedItem;
import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Equipment;
import com.andrei.demo.model.EquipmentQuantityDTO;
import com.andrei.demo.model.LoanRecord;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.StudentLoanRequestDTO;
import com.andrei.demo.repository.EquipmentRepository;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.service.LoanRecordService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates the Project Assistant feature end-to-end.
 *
 * <p>Phase 6 additions: per-user rate limiting via {@link RateLimiterService}
 * (TOO_MANY_REQUESTS on overflow) and full audit logging via {@link
 * AssistantAuditService} for both successful and failed recommendations.
 */
@Service
@AllArgsConstructor
@Slf4j
public class ProjectAssistantService {

    private static final String RATE_LIMIT_OPERATION = "/assistant/recommend";

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final RecommendationParser parser;
    private final List<RecommendationValidator> validators;
    private final EquipmentRepository equipmentRepository;
    private final PersonRepository personRepository;
    private final RecommendationSessionRepository sessionRepository;
    private final LoanRecordService loanRecordService;
    private final RateLimiterService rateLimiterService;
    private final AssistantAuditService auditService;

    @Transactional
    public RecommendationSession recommend(String studentEmail, RecommendationRequest req)
            throws ValidationException {

        // Rate limit FIRST — don't waste DB lookups on rejected requests.
        // Rate-limit failures throw 429 directly; not audited (they never reached the LLM).
        rateLimiterService.enforceLimit(studentEmail, RATE_LIMIT_OPERATION);

        long startMs = System.currentTimeMillis();
        LlmCallResult callResult = null;

        try {
            Person student = personRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new ValidationException(
                            "Person with email " + studentEmail + " not found"));

            List<Equipment> catalog = equipmentRepository.findAll();
            if (catalog.isEmpty()) {
                throw new ValidationException(
                        "Equipment catalog is empty; cannot generate recommendations.");
            }

            // 1. Prompt → LLM (rich call captures latency + token usage)
            PromptBuilder.Prompt prompt = promptBuilder.build(
                    req.description(), catalog, req.expectedReturnDate());
            callResult = llmClient.completeRich(prompt.system(), prompt.user());

            // 2. Parse + validator chain
            RawRecommendation parsed = parser.parse(callResult.content());
            List<ValidatedItem> validated = applyValidatorChain(parsed.items(), catalog);

            // 3. Persist as DRAFT session
            RecommendationSession session = new RecommendationSession();
            session.setPerson(student);
            session.setOriginalDescription(req.description());
            session.setProjectPlan(parsed.projectPlan());
            session.setStatus(RecommendationSessionStatus.DRAFT);
            session.setCreatedAt(Instant.now());
            session.setExpectedReturnDate(req.expectedReturnDate());

            for (ValidatedItem v : validated) {
                RecommendedItem ri = new RecommendedItem();
                ri.setSession(session);
                ri.setEquipment(v.equipment());
                ri.setOriginalLlmName(v.originalLlmName());
                ri.setQuantity(v.quantity());
                ri.setReason(v.reason());
                ri.setAvailabilityStatus(v.availabilityStatus());
                session.getItems().add(ri);
            }

            RecommendationSession saved = sessionRepository.save(session);
            log.info("Created recommendation session {} for {} with {} item(s)",
                    saved.getId(), studentEmail, saved.getItems().size());

            // 4. Audit (separate transaction; best effort)
            auditService.recordSuccess(studentEmail, req, callResult, saved, validated);

            return saved;
        } catch (RuntimeException e) {
            long latencyMs = callResult != null
                    ? callResult.latencyMs()
                    : System.currentTimeMillis() - startMs;
            auditService.recordFailure(studentEmail, req, callResult, e.getMessage(), latencyMs);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "LLM provider failed", e);
        }
    }

    @Transactional(readOnly = true)
    public RecommendationSession getSession(UUID sessionId, String email) {
        return sessionRepository.findById(sessionId)
                .filter(s -> s.getPerson().getEmail().equals(email))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Recommendation session not found"));
    }

    @Transactional
    public RecommendationSession submit(UUID sessionId, String email, RecommendationSubmitRequest req)
            throws ValidationException {
        RecommendationSession session = getSession(sessionId, email);

        if (session.getStatus() != RecommendationSessionStatus.DRAFT) {
            throw new ValidationException(
                    "Recommendation session is " + session.getStatus()
                            + "; only DRAFT sessions can be submitted.");
        }

        Map<UUID, RecommendedItem> sessionItemsById = session.getItems().stream()
                .collect(Collectors.toMap(RecommendedItem::getId, i -> i));

        List<EquipmentQuantityDTO> equipmentQuantities = new ArrayList<>();
        for (RecommendationSubmitRequest.SubmitItem submitItem : req.items()) {
            RecommendedItem sessionItem = sessionItemsById.get(submitItem.recommendedItemId());
            if (sessionItem == null) {
                throw new ValidationException(
                        "Item " + submitItem.recommendedItemId()
                                + " is not part of this session.");
            }
            if (sessionItem.getEquipment() == null) {
                throw new ValidationException(
                        "Item " + submitItem.recommendedItemId()
                                + " could not be matched to a catalog entry and cannot be submitted.");
            }
            EquipmentQuantityDTO eqQty = new EquipmentQuantityDTO();
            eqQty.setEquipmentId(sessionItem.getEquipment().getId());
            eqQty.setQuantity(submitItem.quantity());
            equipmentQuantities.add(eqQty);
        }

        StudentLoanRequestDTO loanReq = new StudentLoanRequestDTO();
        loanReq.setEquipmentQuantities(equipmentQuantities);
        loanReq.setExpectedReturnDate(
                req.expectedReturnDate() != null
                        ? req.expectedReturnDate()
                        : session.getExpectedReturnDate());

        LoanRecord loan = loanRecordService.requestLoan(loanReq, email);

        session.setStatus(RecommendationSessionStatus.SUBMITTED);
        session.setLoanRecord(loan);
        RecommendationSession updated = sessionRepository.save(session);

        log.info("Submitted recommendation session {} as loan record {}", sessionId, loan.getId());
        return updated;
    }

    private List<ValidatedItem> applyValidatorChain(List<RawRecommendedItem> rawItems, List<Equipment> catalog) {
        List<ValidatedItem> items = rawItems.stream()
                .map(r -> new ValidatedItem(r.name(), null, r.quantity(), r.reason(), null))
                .collect(Collectors.toList());
        for (RecommendationValidator v : validators) {
            items = v.validate(items, catalog);
        }
        return items;
    }
}