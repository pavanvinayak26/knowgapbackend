package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String text;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
}
