package com.knowgap.knowgap.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "user_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private QuizAttempt quizAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Question question;

    @Column(length = 1)
    private String selectedOption; // 'A', 'B', 'C', or 'D'

    @Column(nullable = false)
    private Boolean isCorrect;
}
