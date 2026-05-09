package com.andrei.demo.assistant.repository;

import com.andrei.demo.assistant.model.AssistantAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssistantAuditLogRepository extends JpaRepository<AssistantAuditLog, UUID> {

    /** Most recent audit rows for a given user, newest first. */
    List<AssistantAuditLog> findByPersonEmailOrderByCreatedAtDesc(String email);
}