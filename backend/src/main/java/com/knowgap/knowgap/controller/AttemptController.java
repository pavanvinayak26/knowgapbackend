package com.knowgap.knowgap.controller;

import com.knowgap.knowgap.model.Question;
import com.knowgap.knowgap.model.QuizAttempt;
import com.knowgap.knowgap.model.RecallAttempt;
import com.knowgap.knowgap.model.Subject;
import com.knowgap.knowgap.model.Topic;
import com.knowgap.knowgap.model.User;
import com.knowgap.knowgap.model.UserAnswer;
import com.knowgap.knowgap.payload.request.SubmitAttemptRequest;
import com.knowgap.knowgap.payload.request.SubmitRecallAttemptRequest;
import com.knowgap.knowgap.payload.response.AttemptInsightsResponse;
import com.knowgap.knowgap.payload.response.AttemptResultResponse;
import com.knowgap.knowgap.payload.response.AttemptSummaryResponse;
import com.knowgap.knowgap.payload.response.HeatmapResponse;
import com.knowgap.knowgap.payload.response.LearnedSubjectSummaryResponse;
import com.knowgap.knowgap.payload.response.PracticeQuestionResponse;
import com.knowgap.knowgap.payload.response.PracticeSetResponse;
import com.knowgap.knowgap.payload.response.QuestionReviewResponse;
import com.knowgap.knowgap.payload.response.RecallAttemptSummaryResponse;
import com.knowgap.knowgap.payload.response.RecallCheckResponse;
import com.knowgap.knowgap.payload.response.RecallQuestionResponse;
import com.knowgap.knowgap.payload.response.TopicTrendResponse;
import com.knowgap.knowgap.payload.response.WeakTopicResponse;
import com.knowgap.knowgap.repository.QuestionRepository;
import com.knowgap.knowgap.repository.QuizAttemptRepository;
import com.knowgap.knowgap.repository.RecallAttemptRepository;
import com.knowgap.knowgap.repository.SubjectRepository;
import com.knowgap.knowgap.repository.TopicRepository;
import com.knowgap.knowgap.repository.UserAnswerRepository;
import com.knowgap.knowgap.repository.UserRepository;
import com.knowgap.knowgap.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    // Explicit learning analytics thresholds.
    private static final double WEAK_TOPIC_THRESHOLD = 0.5;
    private static final double MASTERY_WEAK_MAX = 0.4;
    private static final double MASTERY_MODERATE_MAX = 0.7;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private RecallAttemptRepository recallAttemptRepository;

    private User validateUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitAttempt(@RequestBody SubmitAttemptRequest request) {
        try {
            User user = validateUser();
            Topic topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new RuntimeException("Topic not found"));

            List<Question> questions = questionRepository.findByTopicId(topic.getId());
            Map<Long, String> submittedAnswers = request.getAnswers() == null ? Collections.emptyMap() : request.getAnswers();

            int score = 0;
            List<UserAnswer> answers = new ArrayList<>();

            QuizAttempt attempt = new QuizAttempt();
            attempt.setUser(user);
            attempt.setTopic(topic);
            attempt.setTotalQuestions(questions.size());
            attempt.setScore(0);
            QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

            for (Question question : questions) {
                String selectedOption = normalizeOption(submittedAnswers.get(question.getId()));
                boolean isCorrect = selectedOption != null && selectedOption.equalsIgnoreCase(question.getCorrectOption());
                if (isCorrect) {
                    score++;
                }

                UserAnswer userAnswer = new UserAnswer();
                userAnswer.setQuizAttempt(savedAttempt);
                userAnswer.setQuestion(question);
                userAnswer.setSelectedOption(selectedOption);
                userAnswer.setIsCorrect(isCorrect);
                answers.add(userAnswer);
            }

            savedAttempt.setScore(score);
            savedAttempt = quizAttemptRepository.save(savedAttempt);
            List<UserAnswer> persistedAnswers = userAnswerRepository.saveAll(answers);

            AttemptResultResponse response = buildAttemptResultResponse(savedAttempt, persistedAnswers, "Quiz submitted successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error: ").append(e).append(" | ");
            if (e.getStackTrace().length > 0) {
                sb.append("At: ").append(e.getStackTrace()[0]);
            }
            return ResponseEntity.internalServerError().body(sb.toString());
        }
    }

    @GetMapping("/heatmap")
    public ResponseEntity<List<HeatmapResponse>> getHeatmap() {
        User user = validateUser();
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByAttemptDateDesc(user.getId());
        return ResponseEntity.ok(toHeatmap(attempts));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<String>> getRecommendations() {
        User user = validateUser();
        List<WeakTopicResponse> weakTopics = calculateWeakTopics(user.getId(), 6);

        List<String> recommendations = weakTopics.stream()
                .map(WeakTopicResponse::getTopicName)
                .collect(Collectors.toList());

        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AttemptSummaryResponse>> getAttemptHistory(@RequestParam(defaultValue = "8") Integer limit) {
        User user = validateUser();
        int boundedLimit = Math.max(1, Math.min(limit, 20));

        List<AttemptSummaryResponse> history = quizAttemptRepository.findByUserIdOrderByAttemptDateDesc(user.getId())
                .stream()
                .limit(boundedLimit)
                .map(this::toAttemptSummary)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    @GetMapping("/{attemptId}/review")
    public ResponseEntity<AttemptResultResponse> getAttemptReview(@PathVariable Long attemptId) {
        User user = validateUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        List<UserAnswer> answers = userAnswerRepository.findByQuizAttemptId(attemptId);
        return ResponseEntity.ok(buildAttemptResultResponse(attempt, answers, "Attempt review loaded."));
    }

    @GetMapping("/insights")
    public ResponseEntity<AttemptInsightsResponse> getInsights() {
        User user = validateUser();
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByAttemptDateDesc(user.getId());

        AttemptInsightsResponse insights = new AttemptInsightsResponse(
                toHeatmap(attempts),
                calculateWeakTopics(user.getId(), 6),
                buildTopicTrends(attempts, 6),
                attempts.stream().limit(6).map(this::toAttemptSummary).collect(Collectors.toList())
        );

        return ResponseEntity.ok(insights);
    }

    @GetMapping("/practice/weak")
    public ResponseEntity<PracticeSetResponse> getWeakPracticeSet(@RequestParam(defaultValue = "8") Integer limit) {
        User user = validateUser();
        int boundedLimit = Math.max(3, Math.min(limit, 20));
        List<WeakTopicResponse> weakTopics = calculateWeakTopics(user.getId(), 3);

        if (weakTopics.isEmpty()) {
            return ResponseEntity.ok(new PracticeSetResponse(
                    "No major weak topics detected. Keep practicing mixed quizzes to maintain momentum.",
                    Collections.emptyList()
            ));
        }

        List<Long> weakTopicIds = weakTopics.stream().map(WeakTopicResponse::getTopicId).collect(Collectors.toList());
        List<Question> candidateQuestions = questionRepository.findByTopicIdIn(weakTopicIds);
        Collections.shuffle(candidateQuestions);

        List<PracticeQuestionResponse> practiceQuestions = candidateQuestions.stream()
                .limit(boundedLimit)
                .map(q -> new PracticeQuestionResponse(
                        q.getId(),
                        q.getText(),
                        q.getOptionA(),
                        q.getOptionB(),
                        q.getOptionC(),
                        q.getOptionD(),
                        q.getTopic().getId(),
                        q.getTopic().getName()
                ))
                .collect(Collectors.toList());

        String focusReason = "Focus topics: " + weakTopics.stream()
                .map(WeakTopicResponse::getTopicName)
                .collect(Collectors.joining(", "));

        return ResponseEntity.ok(new PracticeSetResponse(focusReason, practiceQuestions));
    }

    @GetMapping("/learned/subjects")
    public ResponseEntity<List<LearnedSubjectSummaryResponse>> getLearnedSubjects() {
        User user = validateUser();
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByAttemptDateDesc(user.getId());

        Map<Long, SubjectAggregate> subjectStats = new LinkedHashMap<>();
        for (QuizAttempt attempt : attempts) {
            if (attempt.getTopic() == null || attempt.getTopic().getSubject() == null) {
                continue;
            }

            Long subjectId = attempt.getTopic().getSubject().getId();
            String subjectName = attempt.getTopic().getSubject().getName();

            SubjectAggregate aggregate = subjectStats.computeIfAbsent(
                    subjectId,
                    key -> new SubjectAggregate(subjectId, subjectName)
            );

            aggregate.attemptsCount++;
            aggregate.totalQuestions += attempt.getTotalQuestions();
            aggregate.totalCorrect += attempt.getScore();
            aggregate.learnedTopicIds.add(attempt.getTopic().getId());
            if (aggregate.lastAttemptAt == null || aggregate.lastAttemptAt.isBefore(attempt.getAttemptDate())) {
                aggregate.lastAttemptAt = attempt.getAttemptDate();
            }
        }

        List<LearnedSubjectSummaryResponse> response = subjectStats.values().stream()
                .map(stat -> new LearnedSubjectSummaryResponse(
                        stat.subjectId,
                        stat.subjectName,
                        stat.learnedTopicIds.size(),
                        stat.attemptsCount,
                        stat.totalQuestions == 0 ? 0.0 : roundToOneDecimal((stat.totalCorrect * 100.0) / stat.totalQuestions),
                        stat.lastAttemptAt
                ))
                .sorted(Comparator.comparing(LearnedSubjectSummaryResponse::getLastAttemptAt).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recall/subject/{subjectId}")
    public ResponseEntity<RecallCheckResponse> getSubjectRecallCheck(
            @PathVariable Long subjectId,
            @RequestParam(defaultValue = "8") Integer limit
    ) {
        User user = validateUser();
        int boundedLimit = Math.max(4, Math.min(limit, 12));

        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByAttemptDateDesc(user.getId())
                .stream()
                .filter(attempt -> attempt.getTopic() != null
                        && attempt.getTopic().getSubject() != null
                        && subjectId.equals(attempt.getTopic().getSubject().getId()))
                .collect(Collectors.toList());

        Set<Long> topicIds = new HashSet<>();
        Set<String> learnedTopics = new LinkedHashSet<>();
        String subjectName = null;

        for (QuizAttempt attempt : attempts) {
            topicIds.add(attempt.getTopic().getId());
            learnedTopics.add(attempt.getTopic().getName());
            if (subjectName == null) {
                subjectName = attempt.getTopic().getSubject().getName();
            }
        }

        if (topicIds.isEmpty()) {
            List<Topic> subjectTopics = topicRepository.findBySubjectId(subjectId);
            for (Topic topic : subjectTopics) {
                topicIds.add(topic.getId());
            }
            if (!subjectTopics.isEmpty()) {
                subjectName = subjectTopics.get(0).getSubject().getName();
            }
        }

        List<RecallQuestionResponse> questions = Collections.emptyList();
        if (!topicIds.isEmpty()) {
            List<Question> candidateQuestions = questionRepository.findByTopicIdIn(new ArrayList<>(topicIds));
            Collections.shuffle(candidateQuestions);
            questions = candidateQuestions.stream()
                    .limit(boundedLimit)
                .map(q -> new RecallQuestionResponse(
                            q.getId(),
                            q.getText(),
                            q.getOptionA(),
                            q.getOptionB(),
                            q.getOptionC(),
                            q.getOptionD(),
                    q.getCorrectOption(),
                            q.getTopic().getId(),
                            q.getTopic().getName()
                    ))
                    .collect(Collectors.toList());
        }

        String prompt = learnedTopics.isEmpty()
                ? "You have not attempted this subject yet. Try this recall set to build baseline memory."
                : "Recall check based on what you learned in: " + String.join(", ", learnedTopics);

        return ResponseEntity.ok(new RecallCheckResponse(
                subjectId,
                subjectName == null ? "Selected Subject" : subjectName,
                prompt,
                new ArrayList<>(learnedTopics),
                questions
        ));
    }

    @PostMapping("/recall/submit")
    public ResponseEntity<?> submitRecallAttempt(@RequestBody SubmitRecallAttemptRequest request) {
        try {
            User user = validateUser();
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("Subject not found"));

            Map<Long, String> answers = request.getAnswers() == null ? Collections.emptyMap() : request.getAnswers();
            if (answers.isEmpty()) {
                return ResponseEntity.badRequest().body("Please answer at least one recall question.");
            }

            Set<Long> subjectTopicIds = topicRepository.findBySubjectId(subject.getId()).stream()
                    .map(Topic::getId)
                    .collect(Collectors.toSet());

            List<Question> validQuestions = questionRepository.findAllById(answers.keySet()).stream()
                    .filter(q -> q.getTopic() != null && subjectTopicIds.contains(q.getTopic().getId()))
                    .collect(Collectors.toList());

            if (validQuestions.isEmpty()) {
                return ResponseEntity.badRequest().body("No valid recall questions were submitted for this subject.");
            }

            int score = 0;
            for (Question question : validQuestions) {
                String selected = normalizeOption(answers.get(question.getId()));
                if (selected != null && selected.equalsIgnoreCase(normalizeOption(question.getCorrectOption()))) {
                    score++;
                }
            }

            int total = validQuestions.size();
            double accuracy = total == 0 ? 0.0 : roundToOneDecimal((score * 100.0) / total);

            RecallAttempt recallAttempt = new RecallAttempt();
            recallAttempt.setUser(user);
            recallAttempt.setSubject(subject);
            recallAttempt.setScore(score);
            recallAttempt.setTotalQuestions(total);
            recallAttempt.setAccuracy(accuracy);
            recallAttempt.setRememberedNotes(fallback(request.getRememberedNotes(), ""));
            recallAttempt.setRustyNotes(fallback(request.getRustyNotes(), ""));

            RecallAttempt saved = recallAttemptRepository.save(recallAttempt);
            return ResponseEntity.ok(toRecallSummary(saved));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to submit recall attempt: " + e.getMessage());
        }
    }

    @GetMapping("/recall/history")
    public ResponseEntity<List<RecallAttemptSummaryResponse>> getRecallHistory(@RequestParam(defaultValue = "6") Integer limit) {
        User user = validateUser();
        int boundedLimit = Math.max(1, Math.min(limit, 20));

        List<RecallAttemptSummaryResponse> history = recallAttemptRepository.findByUserIdOrderByAttemptDateDesc(user.getId())
                .stream()
                .limit(boundedLimit)
                .map(this::toRecallSummary)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    private AttemptResultResponse buildAttemptResultResponse(QuizAttempt attempt, List<UserAnswer> answers, String message) {
        int totalQuestions = attempt.getTotalQuestions() == null ? 0 : attempt.getTotalQuestions();
        int score = attempt.getScore() == null ? 0 : attempt.getScore();
        double accuracy = totalQuestions == 0 ? 0.0 : roundToOneDecimal((score * 100.0) / totalQuestions);

        List<QuestionReviewResponse> reviews = answers.stream()
                .map(this::toQuestionReview)
                .sorted(Comparator.comparing(QuestionReviewResponse::getQuestionId))
                .collect(Collectors.toList());

        List<WeakTopicResponse> weakTopics = calculateWeakTopics(attempt.getUser().getId(), 4);
        List<String> nextSteps = buildNextSteps(reviews, weakTopics);

        return new AttemptResultResponse(
                attempt.getId(),
                score,
                totalQuestions,
                accuracy,
                gradeForAccuracy(accuracy),
                message,
                reviews,
                weakTopics,
                nextSteps
        );
    }

    private List<String> buildNextSteps(List<QuestionReviewResponse> reviews, List<WeakTopicResponse> weakTopics) {
        List<String> steps = new ArrayList<>();
        long wrongCount = reviews.stream().filter(r -> !Boolean.TRUE.equals(r.getCorrect())).count();

        if (wrongCount > 0) {
            steps.add("Review the " + wrongCount + " incorrect questions and understand why each correct option works.");
        } else {
            steps.add("Great attempt. Keep consistency by taking one mixed-topic quiz daily.");
        }

        if (!weakTopics.isEmpty()) {
            WeakTopicResponse topWeak = weakTopics.get(0);
            steps.add("Prioritize topic: " + topWeak.getTopicName() + " (" + topWeak.getAccuracy() + "% accuracy).");
        }

        steps.add("Retake a quiz on weak topics after revision to lock in improvement.");
        return steps;
    }

    private QuestionReviewResponse toQuestionReview(UserAnswer answer) {
        Question question = answer.getQuestion();
        Topic topic = question.getTopic();

        String selected = normalizeOption(answer.getSelectedOption());
        String correct = normalizeOption(question.getCorrectOption());
        String selectedText = optionText(question, selected);
        String correctText = optionText(question, correct);
        boolean isCorrect = Boolean.TRUE.equals(answer.getIsCorrect());

        String aiExplanation = buildDetailedExplanation(
            selected,
            selectedText,
            correct,
            correctText,
            topic.getName(),
            isCorrect
        );

        String studyAction = isCorrect
                ? "Attempt one higher-difficulty variation from this topic."
                : "Study " + topic.getName() + " and reattempt this type of question within 24 hours.";

        return new QuestionReviewResponse(
                question.getId(),
                question.getText(),
                selected,
                selectedText,
                correct,
                correctText,
                isCorrect,
                topic.getId(),
                topic.getName(),
                aiExplanation,
                studyAction
        );
    }

    private String buildDetailedExplanation(
            String selected,
            String selectedText,
            String correct,
            String correctText,
            String topicName,
            boolean isCorrect
    ) {
        String concept = "Concept: " + topicName + " core principle.";
        String whyCorrect = "Why correct: Option " + fallback(correct, "N/A") + " is correct because it directly states \""
                + fallback(correctText, "") + "\", which best satisfies the question requirement in this topic.";

        if (isCorrect) {
            return concept + " " + whyCorrect;
        }

        if (selected == null) {
            return concept + " " + whyCorrect + " Why wrong: No option was selected, so the required concept was not demonstrated.";
        }

        String whyWrong = "Why wrong: Option " + selected + " (\"" + fallback(selectedText, "")
                + "\") is weaker because it does not align with the required " + topicName + " concept as directly as option "
                + fallback(correct, "N/A") + ".";

        return concept + " " + whyCorrect + " " + whyWrong;
    }

    private List<HeatmapResponse> toHeatmap(List<QuizAttempt> attempts) {
        Map<String, int[]> topicStats = new LinkedHashMap<>();

        for (QuizAttempt attempt : attempts) {
            String topicName = attempt.getTopic().getName();
            topicStats.putIfAbsent(topicName, new int[]{0, 0});
            topicStats.get(topicName)[0] += attempt.getTotalQuestions();
            topicStats.get(topicName)[1] += attempt.getScore();
        }

        List<HeatmapResponse> heatmap = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : topicStats.entrySet()) {
            int total = entry.getValue()[0];
            int correct = entry.getValue()[1];
            double successRate = total == 0 ? 0.0 : roundToOneDecimal(((double) correct / total) * 100.0);
            heatmap.add(new HeatmapResponse(entry.getKey(), total, correct, successRate));
        }
        return heatmap;
    }

    private List<WeakTopicResponse> calculateWeakTopics(Long userId, int limit) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByAttemptDateDesc(userId);
        Map<Long, TopicAggregate> topicStats = new HashMap<>();

        for (QuizAttempt attempt : attempts) {
            Topic topic = attempt.getTopic();
            TopicAggregate stat = topicStats.computeIfAbsent(topic.getId(), key -> new TopicAggregate(topic.getId(), topic.getName()));
            stat.attempted += attempt.getTotalQuestions();
            stat.correct += attempt.getScore();
        }

        return topicStats.values().stream()
                .map(stat -> {
                    int wrong = Math.max(stat.attempted - stat.correct, 0);
                double accuracyRatio = stat.attempted == 0 ? 0.0 : (double) stat.correct / stat.attempted;
                double accuracy = roundToOneDecimal(accuracyRatio * 100.0);
                double masteryScore = roundToThreeDecimals(accuracyRatio);
                String masteryLevel = masteryLevel(masteryScore);
                boolean weak = accuracyRatio < WEAK_TOPIC_THRESHOLD;
                    return new WeakTopicResponse(
                            stat.topicId,
                            stat.topicName,
                            stat.attempted,
                            stat.correct,
                            wrong,
                            accuracy,
                    roundToThreeDecimals(accuracyRatio),
                    masteryScore,
                    masteryLevel,
                    weak,
                    weakTopicRecommendation(accuracyRatio, wrong, masteryLevel)
                    );
                })
            .filter(stat -> Boolean.TRUE.equals(stat.getWeak()))
                .sorted(Comparator.comparing(WeakTopicResponse::getAccuracy)
                        .thenComparing(WeakTopicResponse::getWrong, Comparator.reverseOrder()))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    private List<TopicTrendResponse> buildTopicTrends(List<QuizAttempt> attempts, int limit) {
        Map<Long, List<QuizAttempt>> attemptsByTopic = new HashMap<>();
        for (QuizAttempt attempt : attempts) {
            attemptsByTopic.computeIfAbsent(attempt.getTopic().getId(), key -> new ArrayList<>()).add(attempt);
        }

        return attemptsByTopic.values().stream()
                .filter(topicAttempts -> !topicAttempts.isEmpty())
                .map(topicAttempts -> {
                    QuizAttempt latest = topicAttempts.get(0);
                    double latestAccuracy = accuracy(latest);
                    double previousAccuracy = topicAttempts.size() > 1 ? accuracy(topicAttempts.get(1)) : latestAccuracy;
                    double delta = roundToOneDecimal(latestAccuracy - previousAccuracy);

                    String recommendation = delta >= 0
                            ? "Progress is improving. Increase challenge with mixed-difficulty questions."
                            : "Performance dipped. Revisit fundamentals before your next timed attempt.";

                    return new TopicTrendResponse(
                            latest.getTopic().getId(),
                            latest.getTopic().getName(),
                            latestAccuracy,
                            previousAccuracy,
                            delta,
                            delta >= 0,
                            topicAttempts.size(),
                            recommendation
                    );
                })
                .sorted(Comparator.comparing(TopicTrendResponse::getDelta))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    private AttemptSummaryResponse toAttemptSummary(QuizAttempt attempt) {
        double accuracy = accuracy(attempt);
        return new AttemptSummaryResponse(
                attempt.getId(),
                attempt.getTopic().getId(),
                attempt.getTopic().getName(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                accuracy,
                gradeForAccuracy(accuracy),
                attempt.getAttemptDate()
        );
    }

    private RecallAttemptSummaryResponse toRecallSummary(RecallAttempt attempt) {
        return new RecallAttemptSummaryResponse(
                attempt.getId(),
                attempt.getSubject().getId(),
                attempt.getSubject().getName(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getAccuracy(),
                attempt.getRememberedNotes(),
                attempt.getRustyNotes(),
                attempt.getAttemptDate()
        );
    }

    private String gradeForAccuracy(double accuracy) {
        if (accuracy >= 90) {
            return "A";
        }
        if (accuracy >= 80) {
            return "B";
        }
        if (accuracy >= 70) {
            return "C";
        }
        if (accuracy >= 60) {
            return "D";
        }
        return "F";
    }

    private String weakTopicRecommendation(double accuracyRatio, int wrong, String masteryLevel) {
        if (accuracyRatio < 0.5) {
            return "High-priority revision needed. Read topic notes and solve 5 guided questions.";
        }
        if ("Moderate".equalsIgnoreCase(masteryLevel)) {
            return "Reinforce this topic with concept recap and timed practice.";
        }
        if (wrong >= 2) {
            return "Do one focused practice set to eliminate recurring mistakes.";
        }
        return "Maintain consistency with light revision.";
    }

    private String masteryLevel(double masteryScore) {
        if (masteryScore < MASTERY_WEAK_MAX) {
            return "Weak";
        }
        if (masteryScore < MASTERY_MODERATE_MAX) {
            return "Moderate";
        }
        return "Strong";
    }

    private String normalizeOption(String option) {
        if (option == null) {
            return null;
        }
        String value = option.trim().toUpperCase(Locale.ROOT);
        if (value.length() != 1 || "ABCD".indexOf(value.charAt(0)) < 0) {
            return null;
        }
        return value;
    }

    private String optionText(Question question, String option) {
        if (option == null) {
            return "Not attempted";
        }

        return switch (option) {
            case "A" -> question.getOptionA();
            case "B" -> question.getOptionB();
            case "C" -> question.getOptionC();
            case "D" -> question.getOptionD();
            default -> "Not attempted";
        };
    }

    private double accuracy(QuizAttempt attempt) {
        if (attempt.getTotalQuestions() == null || attempt.getTotalQuestions() == 0) {
            return 0.0;
        }
        return roundToOneDecimal((attempt.getScore() * 100.0) / attempt.getTotalQuestions());
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double roundToThreeDecimals(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static class TopicAggregate {
        private final Long topicId;
        private final String topicName;
        private int attempted;
        private int correct;

        private TopicAggregate(Long topicId, String topicName) {
            this.topicId = topicId;
            this.topicName = topicName;
            this.attempted = 0;
            this.correct = 0;
        }
    }

    private static class SubjectAggregate {
        private final Long subjectId;
        private final String subjectName;
        private final Set<Long> learnedTopicIds;
        private int attemptsCount;
        private int totalQuestions;
        private int totalCorrect;
        private java.time.LocalDateTime lastAttemptAt;

        private SubjectAggregate(Long subjectId, String subjectName) {
            this.subjectId = subjectId;
            this.subjectName = subjectName;
            this.learnedTopicIds = new HashSet<>();
            this.attemptsCount = 0;
            this.totalQuestions = 0;
            this.totalCorrect = 0;
            this.lastAttemptAt = null;
        }
    }
}
