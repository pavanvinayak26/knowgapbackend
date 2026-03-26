package com.knowgap.knowgap.repository;

import com.knowgap.knowgap.model.RecallAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecallAttemptRepository extends JpaRepository<RecallAttempt, Long> {
    List<RecallAttempt> findByUserIdOrderByAttemptDateDesc(Long userId);
}
