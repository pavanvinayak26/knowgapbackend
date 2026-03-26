package com.knowgap.knowgap.repository;

import com.knowgap.knowgap.model.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    List<UserAnswer> findByQuizAttemptId(Long quizAttemptId);
    List<UserAnswer> findByQuizAttemptIdIn(List<Long> quizAttemptIds);
}
