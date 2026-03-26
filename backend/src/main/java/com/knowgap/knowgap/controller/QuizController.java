package com.knowgap.knowgap.controller;

import com.knowgap.knowgap.model.Question;
import com.knowgap.knowgap.model.Topic;
import com.knowgap.knowgap.payload.request.QuestionRequest;
import com.knowgap.knowgap.payload.response.QuestionResponse;
import com.knowgap.knowgap.repository.QuestionRepository;
import com.knowgap.knowgap.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @GetMapping("/topic/{topicId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsForTopic(@PathVariable Long topicId) {
        List<Question> questions = questionRepository.findByTopicId(topicId);
        Collections.shuffle(questions);
        
        List<QuestionResponse> response = questions.stream()
                .map(q -> new QuestionResponse(q.getId(), q.getText(), q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()))
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }

    @PostMapping("/question")
    public ResponseEntity<?> addQuestion(@RequestBody QuestionRequest request) {
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        Question question = new Question();
        question.setText(request.getText());
        question.setOptionA(request.getOptionA());
        question.setOptionB(request.getOptionB());
        question.setOptionC(request.getOptionC());
        question.setOptionD(request.getOptionD());
        question.setCorrectOption(request.getCorrectOption());
        question.setTopic(topic);

        questionRepository.save(question);
        return ResponseEntity.ok("Question added successfully!");
    }
}
