package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionReviewResponse {
    private Long questionId;
    private String questionText;
    private String yourAnswer;
    private String yourAnswerText;
    private String correctAnswer;
    private String correctAnswerText;
    private Boolean correct;
    private Long topicId;
    private String topicName;
    private String aiExplanation;
    private String studyAction;
}
