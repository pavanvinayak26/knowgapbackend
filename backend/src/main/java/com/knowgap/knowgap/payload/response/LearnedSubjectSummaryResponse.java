package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LearnedSubjectSummaryResponse {
    private Long subjectId;
    private String subjectName;
    private int topicsLearned;
    private int attemptsCount;
    private double averageAccuracy;
    private LocalDateTime lastAttemptAt;
}
