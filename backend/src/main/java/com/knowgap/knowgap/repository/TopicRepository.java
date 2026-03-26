package com.knowgap.knowgap.repository;

import com.knowgap.knowgap.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySubjectId(Long subjectId);
    Optional<Topic> findBySubjectIdAndNameIgnoreCase(Long subjectId, String name);
}
