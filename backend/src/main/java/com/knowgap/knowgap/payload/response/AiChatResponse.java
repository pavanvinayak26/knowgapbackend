package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiChatResponse {
    private String reply;
    private String provider;
    private String model;
}
