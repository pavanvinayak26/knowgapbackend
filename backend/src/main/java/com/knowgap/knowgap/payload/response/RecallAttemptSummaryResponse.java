package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RecallAttemptSummaryResponse {
    private Long attemptId;
    private Long subjectId;
    private String subjectName;
    private int score;
    private int totalQuestions;
    private double accuracy;
    private String rememberedNotes;
    private String rustyNotes;
    private LocalDateTime attemptedAt;
}
