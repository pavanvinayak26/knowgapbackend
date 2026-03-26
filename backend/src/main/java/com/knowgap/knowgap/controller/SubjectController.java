package com.knowgap.knowgap.controller;

import com.knowgap.knowgap.model.Subject;
import com.knowgap.knowgap.model.User;
import com.knowgap.knowgap.repository.SubjectRepository;
import com.knowgap.knowgap.repository.UserRepository;
import com.knowgap.knowgap.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

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

    @GetMapping
    public ResponseEntity<List<Subject>> getAllSubjects() {
        User user = validateUser();
        return ResponseEntity.ok(subjectRepository.findByCreatedByIdOrderByNameAsc(user.getId()));
    }

    @PostMapping
    public ResponseEntity<Subject> addSubject(@RequestBody Subject subject) {
        User user = validateUser();
        subject.setCreatedBy(user);
        return ResponseEntity.ok(subjectRepository.save(subject));
    }
}
