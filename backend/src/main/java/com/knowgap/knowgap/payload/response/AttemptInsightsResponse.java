package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttemptInsightsResponse {
    private List<HeatmapResponse> heatmap;
    private List<WeakTopicResponse> weakTopics;
    private List<TopicTrendResponse> topicTrends;
    private List<AttemptSummaryResponse> recentAttempts;
}
