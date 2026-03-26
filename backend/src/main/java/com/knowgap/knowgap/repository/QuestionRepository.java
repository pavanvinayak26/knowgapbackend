package com.knowgap.knowgap.repository;

import com.knowgap.knowgap.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTopicId(Long topicId);
    List<Question> findByTopicIdIn(List<Long> topicIds);
    long countByTopicId(Long topicId);
}
