package com.knowgap.knowgap.payload.request;

import lombok.Data;
import java.util.Map;

@Data
public class SubmitAttemptRequest {
    private Long topicId;
    private Map<Long, String> answers; // Map of Question ID to the selected option ('A', 'B', 'C', or 'D')
}
