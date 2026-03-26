package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeakTopicResponse {
    private Long topicId;
    private String topicName;
    private Integer attempted;
    private Integer correct;
    private Integer wrong;
    private Double accuracy;
    private Double accuracyRatio;
    private Double masteryScore;
    private String masteryLevel;
    private Boolean weak;
    private String recommendation;
}
