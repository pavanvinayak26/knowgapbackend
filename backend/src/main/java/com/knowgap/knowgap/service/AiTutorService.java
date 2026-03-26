package com.knowgap.knowgap.service;

import com.knowgap.knowgap.payload.request.AiTutorRequest;
import com.knowgap.knowgap.payload.response.AiTutorPlanResponse;

public interface AiTutorService {
    AiTutorPlanResponse prepareLearningPlan(AiTutorRequest request);
}
