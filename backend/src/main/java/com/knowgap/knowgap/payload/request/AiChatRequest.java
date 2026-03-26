package com.knowgap.knowgap.payload.request;

import lombok.Data;

@Data
public class AiChatRequest {
    private String message;
    private String subjectName;
    private String currentLevel;
}
