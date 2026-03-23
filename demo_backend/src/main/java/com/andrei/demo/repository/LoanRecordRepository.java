package com.andrei.demo.repository;

import com.andrei.demo.model.LoanRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoanRecordRepository extends JpaRepository<LoanRecord, UUID> {
}