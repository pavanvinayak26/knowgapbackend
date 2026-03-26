package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicTrendResponse {
    private Long topicId;
    private String topicName;
    private Double latestAccuracy;
    private Double previousAccuracy;
    private Double delta;
    private Boolean improved;
    private Integer attemptsTracked;
    private String recommendation;
}
