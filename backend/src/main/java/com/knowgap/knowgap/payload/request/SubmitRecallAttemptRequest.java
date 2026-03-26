package com.knowgap.knowgap.payload.request;

import lombok.Data;

import java.util.Map;

@Data
public class SubmitRecallAttemptRequest {
    private Long subjectId;
    private Map<Long, String> answers;
    private String rememberedNotes;
    private String rustyNotes;
}
