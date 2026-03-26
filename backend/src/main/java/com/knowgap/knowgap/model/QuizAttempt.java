package com.knowgap.knowgap.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "quiz_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Topic topic;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false, updatable = false)
    private LocalDateTime attemptDate = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        attemptDate = LocalDateTime.now();
    }
}
