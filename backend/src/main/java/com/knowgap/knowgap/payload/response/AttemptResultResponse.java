package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttemptResultResponse {
    private Long attemptId;
    private Integer score;
    private Integer totalQuestions;
    private Double accuracy;
    private String grade;
    private String message;
    private List<QuestionReviewResponse> questionReviews;
    private List<WeakTopicResponse> weakTopics;
    private List<String> nextSteps;
}
