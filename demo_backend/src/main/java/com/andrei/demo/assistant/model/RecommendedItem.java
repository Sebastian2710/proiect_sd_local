package com.andrei.demo.assistant.model;

import com.andrei.demo.assistant.validator.AvailabilityStatus;
import com.andrei.demo.model.Equipment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

/**
 * A single recommended item belonging to a {@link RecommendationSession}.
 *
 * <p>Mirrors the {@link com.andrei.demo.assistant.validator.ValidatedItem}
 * runtime shape but persisted to the database. The {@code equipment} FK is
 * nullable because the validator chain may produce items it could not
 * resolve to a catalog entry — those can't be submitted as a loan request,
 * but we still persist them for audit / debugging.
 */
@Entity
@Data
@Table(name = "recommended_item")
public class RecommendedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RecommendationSession session;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @Column(name = "original_llm_name", nullable = false)
    private String originalLlmName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status")
    private AvailabilityStatus availabilityStatus;
}