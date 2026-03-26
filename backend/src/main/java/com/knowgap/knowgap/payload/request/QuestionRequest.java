package com.knowgap.knowgap.payload.request;

import lombok.Data;

@Data
public class QuestionRequest {
    private String text;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption;
    private Long topicId;
}
