package com.knowgap.knowgap.controller;

import com.knowgap.knowgap.model.Topic;
import com.knowgap.knowgap.model.User;
import com.knowgap.knowgap.repository.SubjectRepository;
import com.knowgap.knowgap.repository.TopicRepository;
import com.knowgap.knowgap.repository.UserRepository;
import com.knowgap.knowgap.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    private User validateUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<Topic>> getTopicsBySubject(@PathVariable Long subjectId) {
        User user = validateUser();
        subjectRepository.findByIdAndCreatedById(subjectId, user.getId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        return ResponseEntity.ok(topicRepository.findBySubjectId(subjectId));
    }

    @PostMapping
    public ResponseEntity<Topic> addTopic(@RequestBody Topic topic) {
        User user = validateUser();
        Long subjectId = topic.getSubject() == null ? null : topic.getSubject().getId();
        if (subjectId == null) {
            throw new RuntimeException("Subject is required");
        }
        topic.setSubject(subjectRepository.findByIdAndCreatedById(subjectId, user.getId())
                .orElseThrow(() -> new RuntimeException("Subject not found")));
        return ResponseEntity.ok(topicRepository.save(topic));
    }
}
