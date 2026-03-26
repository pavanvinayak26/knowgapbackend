package com.knowgap.knowgap.controller;

import com.knowgap.knowgap.payload.request.AiChatRequest;
import com.knowgap.knowgap.payload.request.AiTutorRequest;
import com.knowgap.knowgap.payload.response.AiChatResponse;
import com.knowgap.knowgap.payload.response.AiTutorPlanResponse;
import com.knowgap.knowgap.service.AiTutorService;
import com.knowgap.knowgap.service.HuggingFaceChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiTutorController {

    @Autowired
    private AiTutorService aiTutorService;

    @Autowired
    private HuggingFaceChatService huggingFaceChatService;

    @PostMapping("/plan")
    public ResponseEntity<AiTutorPlanResponse> createPlan(@RequestBody AiTutorRequest request) {
        return ResponseEntity.ok(aiTutorService.prepareLearningPlan(request));
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return ResponseEntity.ok(huggingFaceChatService.chat(request));
    }
}
