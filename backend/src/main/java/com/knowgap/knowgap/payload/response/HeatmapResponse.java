package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HeatmapResponse {
    private String topicName;
    private int totalAttempted;
    private int correctAnswers;
    private double successRate; // Percentage
}
