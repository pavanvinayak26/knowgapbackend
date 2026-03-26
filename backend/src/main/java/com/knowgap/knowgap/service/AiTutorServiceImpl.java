package com.knowgap.knowgap.service;

import com.knowgap.knowgap.model.Question;
import com.knowgap.knowgap.model.Subject;
import com.knowgap.knowgap.model.Topic;
import com.knowgap.knowgap.model.User;
import com.knowgap.knowgap.payload.request.AiTutorRequest;
import com.knowgap.knowgap.payload.response.AiTutorPlanResponse;
import com.knowgap.knowgap.payload.response.AiTutorTopicResponse;
import com.knowgap.knowgap.repository.QuestionRepository;
import com.knowgap.knowgap.repository.SubjectRepository;
import com.knowgap.knowgap.repository.TopicRepository;
import com.knowgap.knowgap.repository.UserRepository;
import com.knowgap.knowgap.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AiTutorServiceImpl implements AiTutorService {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    private enum SubjectDomain {
        MATH,
        CODING,
        SCIENCE,
        BUSINESS,
        LANGUAGE,
        GENERAL
    }

    @Override
    @Transactional
    public AiTutorPlanResponse prepareLearningPlan(AiTutorRequest request) {
        User currentUser = validateUser();
        String subjectName = normalizeSubject(request.getSubjectName());
        String learnerGoal = defaultIfBlank(request.getLearnerGoal(), "Build complete practical understanding");
        String level = defaultIfBlank(request.getCurrentLevel(), "beginner");
        int topicTarget = clamp(request.getTopicsCount(), 4, 8);
        int questionTarget = clamp(request.getQuestionsPerTopic(), 5, 8);
        SubjectDomain domain = detectDomain(subjectName, learnerGoal);

        Subject subject = subjectRepository.findByNameIgnoreCaseAndCreatedById(subjectName, currentUser.getId())
                .orElseGet(() -> {
                    Subject s = new Subject();
                    s.setName(subjectName);
                    s.setDescription(buildSubjectSummary(subjectName, learnerGoal, level));
                    s.setCreatedBy(currentUser);
                    return subjectRepository.save(s);
                });

        if (subject.getDescription() == null || subject.getDescription().isBlank()) {
            subject.setDescription(buildSubjectSummary(subjectName, learnerGoal, level));
            subject = subjectRepository.save(subject);
        }

        Subject subjectRef = subject;
        List<String> topicNames = generateTopicNames(subjectName, learnerGoal, level, topicTarget);
        List<AiTutorTopicResponse> topicResponses = new ArrayList<>();

        int moduleIndex = 1;
        for (String topicName : topicNames) {
            Topic topic = topicRepository.findBySubjectIdAndNameIgnoreCase(subjectRef.getId(), topicName)
                    .orElseGet(() -> {
                        Topic t = new Topic();
                        t.setName(topicName);
                        t.setSubject(subjectRef);
                        return topicRepository.save(t);
                    });

            List<Question> existingQuestions = questionRepository.findByTopicId(topic.getId());
            int existingCount = existingQuestions.size();
            int toGenerate = questionTarget - existingCount;

            // If this topic only has older generic template questions, inject fresh subject-aware questions.
            if (looksLikeLegacyGenericSet(existingQuestions)) {
                toGenerate = Math.max(toGenerate, Math.max(3, questionTarget / 2));
            }

            if (toGenerate > 0) {
                List<Question> generated = generateQuestions(subjectName, topicName, learnerGoal, level, moduleIndex, toGenerate, domain);
                for (Question q : generated) {
                    q.setTopic(topic);
                }
                questionRepository.saveAll(generated);
            }

            String overview = buildTopicOverview(topicName, learnerGoal, moduleIndex);
            long finalCount = questionRepository.countByTopicId(topic.getId());
            topicResponses.add(new AiTutorTopicResponse(topic.getId(), topic.getName(), overview, (int) finalCount));
            moduleIndex++;
        }

        String path = buildLearningPath(topicResponses, learnerGoal, level);
        return new AiTutorPlanResponse(subject.getId(), subject.getName(), subject.getDescription(), path, topicResponses);
    }

    private User validateUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String normalizeSubject(String name) {
        String value = defaultIfBlank(name, "General Studies").trim();
        if (value.length() == 1) {
            return value.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String defaultIfBlank(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private int clamp(Integer value, int min, int max) {
        if (value == null) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private String buildSubjectSummary(String subjectName, String learnerGoal, String level) {
        return "AI-generated learning track for " + subjectName + " (" + level + ") with focus on: " + learnerGoal + ".";
    }

    private String buildTopicOverview(String topicName, String learnerGoal, int moduleIndex) {
        return "Module " + moduleIndex + ": " + topicName + " mapped to learner objective: " + learnerGoal + ".";
    }

    private SubjectDomain detectDomain(String subjectName, String learnerGoal) {
        String text = (subjectName + " " + learnerGoal).toLowerCase(Locale.ROOT);

        if (containsAny(text, "math", "mathematics", "algebra", "geometry", "calculus", "statistics", "quant")) {
            return SubjectDomain.MATH;
        }
        if (containsAny(text, "code", "coding", "program", "java", "python", "javascript", "c++", "sql", "algorithm", "data structure", "developer")) {
            return SubjectDomain.CODING;
        }
        if (containsAny(text, "physics", "chemistry", "biology", "science", "electrical", "mechanical")) {
            return SubjectDomain.SCIENCE;
        }
        if (containsAny(text, "finance", "economics", "account", "business", "marketing", "management")) {
            return SubjectDomain.BUSINESS;
        }
        if (containsAny(text, "english", "grammar", "language", "vocabulary", "writing", "ielts", "toefl")) {
            return SubjectDomain.LANGUAGE;
        }
        return SubjectDomain.GENERAL;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeLegacyGenericSet(List<Question> questions) {
        if (questions.isEmpty()) {
            return false;
        }

        long genericCount = questions.stream()
                .map(Question::getText)
                .filter(this::isLegacyGenericQuestion)
                .count();

        return genericCount >= Math.max(2, questions.size() / 2);
    }

    private boolean isLegacyGenericQuestion(String text) {
        String value = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return value.contains("strongest first step")
                || value.contains("which approach best supports the goal")
                || value.contains("most effective way to retain")
                || value.contains("which statement is most accurate for module");
    }

    private String buildLearningPath(List<AiTutorTopicResponse> topics, String learnerGoal, String level) {
        StringBuilder sb = new StringBuilder();
        sb.append("Start at ").append(level).append(" level. Goal: ").append(learnerGoal).append(". ");
        sb.append("Recommended sequence: ");
        for (int i = 0; i < topics.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(topics.get(i).getTopicName());
        }
        sb.append(". After each module, take quiz and revise weak areas.");
        return sb.toString();
    }

    private List<String> generateTopicNames(String subjectName, String learnerGoal, String level, int target) {
        Set<String> topics = new LinkedHashSet<>();
        topics.add(subjectName + " Foundations");
        topics.add("Core Concepts of " + subjectName);
        topics.add(subjectName + " Practical Problem Solving");
        topics.add(subjectName + " Applied Projects");

        String goalLower = learnerGoal.toLowerCase(Locale.ROOT);
        if (goalLower.contains("exam") || goalLower.contains("interview")) {
            topics.add(subjectName + " Interview & Exam Strategy");
        }
        if (goalLower.contains("job") || goalLower.contains("career")) {
            topics.add(subjectName + " Industry Use Cases");
        }

        if ("intermediate".equalsIgnoreCase(level) || "advanced".equalsIgnoreCase(level)) {
            topics.add("Advanced " + subjectName + " Techniques");
        }
        if ("advanced".equalsIgnoreCase(level)) {
            topics.add(subjectName + " Optimization and Expert Patterns");
        }

        topics.addAll(Arrays.asList(
                subjectName + " Troubleshooting",
                subjectName + " Best Practices"
        ));

        List<String> topicList = new ArrayList<>(topics);
        if (topicList.size() > target) {
            return topicList.subList(0, target);
        }
        return topicList;
    }

    private List<Question> generateQuestions(
            String subjectName,
            String topicName,
            String learnerGoal,
            String level,
            int moduleIndex,
            int count,
            SubjectDomain domain
    ) {
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            GeneratedQuestion generated = switch (domain) {
                case MATH -> buildMathQuestion(topicName, moduleIndex, i);
                case CODING -> buildCodingQuestion(topicName, moduleIndex, i);
                case SCIENCE -> buildScienceQuestion(subjectName, topicName, moduleIndex, i);
                case BUSINESS -> buildBusinessQuestion(subjectName, topicName, learnerGoal, i);
                case LANGUAGE -> buildLanguageQuestion(subjectName, topicName, i);
                default -> buildGeneralQuestion(subjectName, topicName, learnerGoal, level, moduleIndex, i);
            };

            Question q = new Question();
            q.setText(generated.text());
            q.setOptionA(generated.optionA());
            q.setOptionB(generated.optionB());
            q.setOptionC(generated.optionC());
            q.setOptionD(generated.optionD());
            q.setCorrectOption(generated.correctOption());
            questions.add(q);
        }
        return questions;
    }

    private GeneratedQuestion buildMathQuestion(String topicName, int moduleIndex, int questionIndex) {
        int pattern = (questionIndex - 1) % 4;
        int seed = moduleIndex * 7 + questionIndex * 3;

        if (pattern == 0) {
            int a = 8 + seed;
            int b = 5 + moduleIndex;
            int c = 2 + (questionIndex % 4);
            int answer = a + (b * c);
            return new GeneratedQuestion(
                    "In " + topicName + ", evaluate: " + a + " + " + b + " x " + c + ".",
                    String.valueOf(answer),
                    String.valueOf((a + b) * c),
                    String.valueOf(answer - 2),
                    String.valueOf(answer + 3),
                    "A"
            );
        }

        if (pattern == 1) {
            int x = 2 + (moduleIndex % 4);
            int rhs = (x * 5) + 10;
            return new GeneratedQuestion(
                    "Solve for x: 5x + 10 = " + rhs + ".",
                    String.valueOf(x - 1),
                    String.valueOf(x),
                    String.valueOf(x + 1),
                    String.valueOf(x + 2),
                    "B"
            );
        }

        if (pattern == 2) {
            int total = 120 + (seed * 2);
            int percent = 20 + (moduleIndex * 5);
            int answer = (total * percent) / 100;
            return new GeneratedQuestion(
                    "What is " + percent + "% of " + total + "?",
                    String.valueOf(answer + 10),
                    String.valueOf(answer - 6),
                    String.valueOf(answer),
                    String.valueOf(answer + 4),
                    "C"
            );
        }

        int numerator = 15 + seed;
        int denominator = 3 + (questionIndex % 4);
        int quotient = numerator / denominator;
        return new GeneratedQuestion(
                "Compute " + numerator + " / " + denominator + " (integer result).",
                String.valueOf(quotient - 1),
                String.valueOf(quotient + 2),
                String.valueOf(quotient + 1),
                String.valueOf(quotient),
                "D"
        );
    }

    private GeneratedQuestion buildCodingQuestion(String topicName, int moduleIndex, int questionIndex) {
        int pattern = (questionIndex - 1) % 4;

        if (pattern == 0) {
            int n = 3 + moduleIndex;
            int result = n * n;
            return new GeneratedQuestion(
                    "For pseudo code `int n = " + n + "; int r = n * n; print(r);` what is printed?",
                    String.valueOf(result - n),
                    String.valueOf(result),
                    String.valueOf(n + n),
                    String.valueOf(result + n),
                    "B"
            );
        }

        if (pattern == 1) {
            return new GeneratedQuestion(
                    "In " + topicName + ", which data structure gives average O(1) key lookup?",
                    "ArrayList",
                    "Linked List",
                    "Hash Map",
                    "Binary Search Tree",
                    "C"
            );
        }

        if (pattern == 2) {
            int start = moduleIndex + questionIndex;
            int endExclusive = start + 4;
            int sum = (start + (endExclusive - 1)) * 4 / 2;
            return new GeneratedQuestion(
                    "What is the output of `sum=0; for(i=" + start + "; i<" + endExclusive + "; i++) sum+=i; print(sum);`?",
                    String.valueOf(sum + 2),
                    String.valueOf(sum - 1),
                    String.valueOf(sum),
                    String.valueOf(sum + 5),
                    "C"
            );
        }

        return new GeneratedQuestion(
                "Which change best improves code quality for repeated logic in " + topicName + "?",
                "Copy the same code block in multiple methods",
                "Increase method length to keep all logic together",
                "Use global mutable state everywhere",
                "Extract a reusable function with clear inputs/outputs",
                "D"
        );
    }

    private GeneratedQuestion buildScienceQuestion(String subjectName, String topicName, int moduleIndex, int questionIndex) {
        int pattern = (questionIndex - 1) % 4;
        if (pattern == 0) {
            return new GeneratedQuestion(
                    "In " + topicName + ", what is the SI unit of force?",
                    "Joule",
                    "Newton",
                    "Pascal",
                    "Watt",
                    "B"
            );
        }
        if (pattern == 1) {
            return new GeneratedQuestion(
                    "A hypothesis in " + subjectName + " should primarily be:",
                    "Untestable",
                    "Based on opinion only",
                    "Testable and falsifiable",
                    "Always proven true",
                    "C"
            );
        }
        if (pattern == 2) {
            return new GeneratedQuestion(
                    "If speed doubles while time stays same, distance becomes:",
                    "Half",
                    "Unchanged",
                    "Double",
                    "Quadruple",
                    "C"
            );
        }
        return new GeneratedQuestion(
                "For module " + moduleIndex + " of " + topicName + ", which approach strengthens understanding most?",
                "Memorize outcomes without experiments",
                "Use controlled experiments and record observations",
                "Skip measurements",
                "Avoid comparing results",
                "B"
        );
    }

    private GeneratedQuestion buildBusinessQuestion(String subjectName, String topicName, String learnerGoal, int questionIndex) {
        int pattern = (questionIndex - 1) % 4;
        if (pattern == 0) {
            return new GeneratedQuestion(
                    "Which metric best reflects profitability in " + topicName + "?",
                    "Net profit margin",
                    "Website visits",
                    "Employee count",
                    "Social media likes",
                    "A"
            );
        }
        if (pattern == 1) {
            return new GeneratedQuestion(
                    "If revenue is 500,000 and costs are 420,000, profit is:",
                    "70,000",
                    "80,000",
                    "90,000",
                    "120,000",
                    "B"
            );
        }
        if (pattern == 2) {
            return new GeneratedQuestion(
                    "For goal '" + learnerGoal + "', which strategy is strongest?",
                    "No customer feedback loop",
                    "One-time campaign only",
                    "Measure, iterate, and segment customers",
                    "Ignore unit economics",
                    "C"
            );
        }
        return new GeneratedQuestion(
                "In " + subjectName + ", what does ROI stand for?",
                "Rate of Income",
                "Return on Inventory",
                "Revenue over Interest",
                "Return on Investment",
                "D"
        );
    }

    private GeneratedQuestion buildLanguageQuestion(String subjectName, String topicName, int questionIndex) {
        int pattern = (questionIndex - 1) % 4;
        if (pattern == 0) {
            return new GeneratedQuestion(
                    "Choose the grammatically correct sentence for " + topicName + ":",
                    "She don't like reading.",
                    "She doesn't likes reading.",
                    "She doesn't like reading.",
                    "She not like reading.",
                    "C"
            );
        }
        if (pattern == 1) {
            return new GeneratedQuestion(
                    "Which word is closest in meaning to 'concise' in " + subjectName + " practice?",
                    "Lengthy",
                    "Brief",
                    "Confusing",
                    "Unclear",
                    "B"
            );
        }
        if (pattern == 2) {
            return new GeneratedQuestion(
                    "Identify the correct punctuation:",
                    "Lets eat, Grandma!",
                    "Let's eat, Grandma!",
                    "Lets, eat Grandma!",
                    "Let's eat Grandma",
                    "B"
            );
        }
        return new GeneratedQuestion(
                "Which improves writing clarity most in " + topicName + "?",
                "Long sentences without breaks",
                "Passive voice in every line",
                "Ambiguous pronouns",
                "Specific words and clear sentence structure",
                "D"
        );
    }

    private GeneratedQuestion buildGeneralQuestion(
            String subjectName,
            String topicName,
            String learnerGoal,
            String level,
            int moduleIndex,
            int questionIndex
    ) {
        int type = (questionIndex - 1) % 4;
        if (type == 0) {
            return new GeneratedQuestion(
                    "In " + topicName + ", what is the strongest first step for a " + level + " learner?",
                    "Ignore fundamentals and jump to advanced modules",
                    "Understand key concepts and map them to simple examples",
                    "Only memorize definitions",
                    "Skip practice tasks",
                    "B"
            );
        }
        if (type == 1) {
            return new GeneratedQuestion(
                    "Which approach best supports the goal: " + learnerGoal + "?",
                    "One-time reading without review",
                    "Practice, feedback, and topic-wise revision",
                    "Random topic switching",
                    "Studying without quizzes",
                    "B"
            );
        }
        if (type == 2) {
            return new GeneratedQuestion(
                    "Which statement is most accurate for module " + moduleIndex + " of " + subjectName + "?",
                    "Concepts should be learned in progressive sequence",
                    "The sequence is irrelevant to mastery",
                    "No need for practical exercises",
                    "Only final tests matter",
                    "A"
            );
        }
        return new GeneratedQuestion(
                "What is the most effective way to retain " + topicName + " long-term?",
                "Read once and move on",
                "Use spaced revision and mixed quizzes",
                "Avoid applied questions",
                "Rely only on video watching",
                "B"
        );
    }

    private record GeneratedQuestion(
            String text,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String correctOption
    ) {
    }
}
