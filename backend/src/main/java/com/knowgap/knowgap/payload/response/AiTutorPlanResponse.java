package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AiTutorPlanResponse {
    private Long subjectId;
    private String subjectName;
    private String subjectSummary;
    private String learningPath;
    private List<AiTutorTopicResponse> topics;
}
