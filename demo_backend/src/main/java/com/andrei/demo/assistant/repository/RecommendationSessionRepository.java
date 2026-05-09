package com.andrei.demo.assistant.repository;

import com.andrei.demo.assistant.model.RecommendationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendationSessionRepository extends JpaRepository<RecommendationSession, UUID> {

    /** All sessions belonging to the given student, by email. */
    List<RecommendationSession> findByPerson_Email(String email);
}