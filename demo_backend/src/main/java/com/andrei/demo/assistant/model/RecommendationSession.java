package com.andrei.demo.assistant.model;

import com.andrei.demo.model.LoanRecord;
import com.andrei.demo.model.Person;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "recommendation_session")
public class RecommendationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "original_description", nullable = false, columnDefinition = "TEXT")
    private String originalDescription;

    @Column(name = "project_plan", columnDefinition = "TEXT")
    private String projectPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationSessionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expected_return_date", nullable = false)
    private LocalDate expectedReturnDate;

    /** Set only after the session is submitted as a loan request. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_record_id")
    private LoanRecord loanRecord;

    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<RecommendedItem> items = new ArrayList<>();
}