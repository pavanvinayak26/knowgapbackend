package com.knowgap.knowgap.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSetResponse {
    private String focusReason;
    private List<PracticeQuestionResponse> questions;
}
