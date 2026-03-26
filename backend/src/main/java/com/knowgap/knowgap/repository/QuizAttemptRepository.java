package com.knowgap.knowgap.repository;

import com.knowgap.knowgap.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserId(Long userId);
    List<QuizAttempt> findByUserIdOrderByAttemptDateDesc(Long userId);
    Optional<QuizAttempt> findByIdAndUserId(Long id, Long userId);
}
