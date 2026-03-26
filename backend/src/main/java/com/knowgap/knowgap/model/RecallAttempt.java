package com.knowgap.knowgap.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "recall_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecallAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Subject subject;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false)
    private Double accuracy;

    @Column(columnDefinition = "TEXT")
    private String rememberedNotes;

    @Column(columnDefinition = "TEXT")
    private String rustyNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime attemptDate = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        attemptDate = LocalDateTime.now();
    }
}
