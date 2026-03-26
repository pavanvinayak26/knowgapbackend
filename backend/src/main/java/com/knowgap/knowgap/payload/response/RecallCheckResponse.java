package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecallCheckResponse {
    private Long subjectId;
    private String subjectName;
    private String prompt;
    private List<String> learnedTopics;
    private List<RecallQuestionResponse> questions;
}
