package com.andrei.demo.assistant.service;

import com.andrei.demo.assistant.client.LlmCallResult;
import com.andrei.demo.assistant.model.AssistantAuditLog;
import com.andrei.demo.assistant.model.RecommendationRequest;
import com.andrei.demo.assistant.model.RecommendationSession;
import com.andrei.demo.assistant.prompt.PromptBuilder;
import com.andrei.demo.assistant.repository.AssistantAuditLogRepository;
import com.andrei.demo.assistant.validator.ValidatedItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists audit rows for every project-assistant LLM call.
 *
 * <p>All write methods use {@code Propagation.REQUIRES_NEW} so the audit
 * row commits independently of the calling transaction — failures in the
 * orchestrator transaction roll back the failed work but the audit row
 * survives, which is exactly what we want for postmortems.
 *
 * <p>Audit writes are best-effort: failures are logged but never propagated
 * to the caller. Losing an audit row should never fail a user request.
 */
@Service
@Slf4j
public class AssistantAuditService {

    private final AssistantAuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final String model;

    public AssistantAuditService(
            AssistantAuditLogRepository repository,
            ObjectMapper objectMapper,
            @Value("${llm.model}") String model
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String email,
                              RecommendationRequest request,
                              LlmCallResult callResult,
                              RecommendationSession session,
                              List<ValidatedItem> validatedItems) {
        try {
            AssistantAuditLog audit = baseAudit(email, request);
            audit.setPersonId(session.getPerson().getId());
            audit.setRawLlmResponse(callResult.content());
            audit.setLatencyMs(callResult.latencyMs());
            audit.setPromptTokens(callResult.promptTokens());
            audit.setCompletionTokens(callResult.completionTokens());
            audit.setSuccess(true);
            audit.setSessionId(session.getId());
            audit.setParsedItemsJson(serializeItems(validatedItems));
            repository.save(audit);
        } catch (Exception e) {
            log.warn("Failed to save success audit log for {}: {}", email, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String email,
                              RecommendationRequest request,
                              LlmCallResult partialCallResult,  // nullable
                              String errorMessage,
                              long latencyMs) {
        try {
            AssistantAuditLog audit = baseAudit(email, request);
            audit.setSuccess(false);
            audit.setErrorMessage(errorMessage);
            audit.setLatencyMs(latencyMs);
            if (partialCallResult != null) {
                audit.setRawLlmResponse(partialCallResult.content());
                audit.setPromptTokens(partialCallResult.promptTokens());
                audit.setCompletionTokens(partialCallResult.completionTokens());
            }
            repository.save(audit);
        } catch (Exception e) {
            log.warn("Failed to save failure audit log for {}: {}", email, e.getMessage());
        }
    }

    private AssistantAuditLog baseAudit(String email, RecommendationRequest request) {
        AssistantAuditLog audit = new AssistantAuditLog();
        audit.setPersonEmail(email);
        audit.setPromptVersion(PromptBuilder.PROMPT_VERSION);
        audit.setModel(model);
        audit.setDescription(request.description());
        audit.setExpectedReturnDate(request.expectedReturnDate());
        audit.setCreatedAt(Instant.now());
        return audit;
    }

    private String serializeItems(List<ValidatedItem> items) {
        try {
            List<Map<String, Object>> projected = items.stream().map(this::project).toList();
            return objectMapper.writeValueAsString(projected);
        } catch (Exception e) {
            log.warn("Failed to serialize parsed items for audit: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> project(ValidatedItem item) {
        Map<String, Object> m = new HashMap<>();
        m.put("originalLlmName", item.originalLlmName());
        m.put("equipmentId", item.equipment() == null ? null : item.equipment().getId().toString());
        m.put("equipmentName", item.equipment() == null ? null : item.equipment().getName());
        m.put("quantity", item.quantity());
        m.put("availabilityStatus",
                item.availabilityStatus() == null ? null : item.availabilityStatus().name());
        return m;
    }
}