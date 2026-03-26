package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttemptSummaryResponse {
    private Long attemptId;
    private Long topicId;
    private String topicName;
    private Integer score;
    private Integer totalQuestions;
    private Double accuracy;
    private String grade;
    private LocalDateTime attemptDate;
}
