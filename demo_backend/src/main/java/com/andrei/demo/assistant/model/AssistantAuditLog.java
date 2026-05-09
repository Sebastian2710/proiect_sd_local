package com.andrei.demo.assistant.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One row per call to the project-assistant pipeline. Records who made the
 * request, what was sent to the LLM, what came back, how long it took, and
 * whether it succeeded.
 *
 * <p>Audit rows are persisted in a separate transaction
 * ({@code Propagation.REQUIRES_NEW}) so they aren't rolled back when the
 * surrounding {@code @Transactional} method fails — that's exactly when the
 * audit row is most useful.
 */
@Entity
@Data
@Table(name = "assistant_audit_log")
public class AssistantAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /** Person ID — nullable in case the email lookup itself fails. */
    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "person_email", nullable = false)
    private String personEmail;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(name = "model")
    private String model;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(name = "raw_llm_response", columnDefinition = "TEXT")
    private String rawLlmResponse;

    /** JSON snapshot of validated items (after the validator chain ran). Null on failure. */
    @Column(name = "parsed_items_json", columnDefinition = "TEXT")
    private String parsedItemsJson;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}