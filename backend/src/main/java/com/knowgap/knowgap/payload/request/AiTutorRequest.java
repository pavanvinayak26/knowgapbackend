package com.knowgap.knowgap.payload.request;

import lombok.Data;

@Data
public class AiTutorRequest {
    private String subjectName;
    private String learnerGoal;
    private String currentLevel;
    private Integer topicsCount;
    private Integer questionsPerTopic;
}
