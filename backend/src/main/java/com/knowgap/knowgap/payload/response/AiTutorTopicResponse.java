package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiTutorTopicResponse {
    private Long topicId;
    private String topicName;
    private String overview;
    private int questionsPrepared;
}
