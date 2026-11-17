package com.interviewpartner.ai.impl;

import com.interviewpartner.ai.WaveAIService;
import com.interviewpartner.ai.dto.AIRequest;
import com.interviewpartner.ai.dto.AIResponse;
import com.interviewpartner.entity.Answer;
import com.interviewpartner.entity.InterviewSession;
import com.interviewpartner.entity.Question;
import com.interviewpartner.interview.enums.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class MockWaveAIService implements WaveAIService {

    private final Map<String, List<String>> roleQuestions = new HashMap<>();

    public MockWaveAIService() {
        initializeQuestions();
    }

    private void initializeQuestions() {
        roleQuestions.put("JAVA", Arrays.asList(
                "Explain the difference between abstract classes and interfaces in Java.",
                "What is the Java memory model and how does garbage collection work?",
                "Explain the Singleton design pattern and its implementations.",
                "What are Java Streams and how do they differ from collections?",
                "Explain the concept of immutability in Java and how to create immutable classes."
        ));

        roleQuestions.put("FRONTEND", Arrays.asList(
                "Explain the React component lifecycle and its phases.",
                "What is the difference between state and props in React?",
                "Explain the Virtual DOM and how it improves performance.",
                "What are React Hooks and why were they introduced?",
                "Explain the concept of Higher-Order Components in React."
        ));

        roleQuestions.put("BACKEND", Arrays.asList(
                "Explain the CAP theorem and its implications for distributed systems.",
                "What is the difference between REST and GraphQL APIs?",
                "Explain database indexing and when to use it.",
                "What are microservices and how do they differ from monolithic architecture?",
                "Explain the ACID properties in database transactions."
        ));

        roleQuestions.put("DSA", Arrays.asList(
                "Explain the time and space complexity of different sorting algorithms.",
                "What is the difference between a HashMap and a TreeMap?",
                "Explain the concept of dynamic programming with an example.",
                "What is the difference between BFS and DFS traversal?",
                "Explain the concept of binary search and its applications."
        ));
    }

    @Override
    public AIResponse generateMCQ(String role, String difficulty, String topic) {
        log.debug("Generating MCQ for role: {}, difficulty: {}, topic: {}", role, difficulty, topic);

        String questionText = String.format("What is a key concept in %s at %s level?", role, difficulty);
        List<String> options = Arrays.asList(
                "Option A - Correct answer",
                "Option B - Alternative",
                "Option C - Distractor",
                "Option D - Another distractor"
        );
        int correctIndex = 0;

        String responseJson = String.format(
                "{\"question\":\"%s\",\"options\":[\"%s\",\"%s\",\"%s\",\"%s\"],\"correctIndex\":%d,\"explanation\":\"This is the correct answer because...\",\"topic\":\"%s\",\"difficulty\":2}",
                questionText, options.get(0), options.get(1), options.get(2), options.get(3), correctIndex, topic
        );

        return AIResponse.builder()
                .content(responseJson)
                .success(true)
                .metadata(Map.of("type", "mcq", "role", role, "difficulty", difficulty))
                .processingTimeMs(100L)
                .build();
    }

    @Override
    public AIResponse generateInterviewQuestion(
            String role,
            String difficulty,
            int questionNumber,
            String topicFocus,
            String conversationContext,
            List<String> previousQuestions
    ) {
        log.debug("Generating interview question for role: {}, difficulty: {}, question: {}", role, difficulty, questionNumber);

        List<String> questions = roleQuestions.getOrDefault(role, roleQuestions.get("DSA"));
        String question = questions.stream()
                .filter(candidate -> previousQuestions == null || !previousQuestions.contains(candidate))
                .findFirst()
                .orElse(questions.get(questionNumber % questions.size()));

        String responseJson = String.format(
                "{\"question\":\"%s\",\"context\":\"Context for the question\",\"topic\":\"%s\",\"difficulty\":2,\"followUp\":true,\"expectedSkills\":[\"Skill 1\",\"Skill 2\"]}",
                question, topicFocus == null || topicFocus.isBlank() ? role : topicFocus
        );

        return AIResponse.builder()
                .content(responseJson)
                .success(true)
                .metadata(Map.of("type", "interview", "role", role, "difficulty", difficulty, "questionNumber", questionNumber))
                .processingTimeMs(80L)
                .build();
    }

    @Override
    public AIResponse generateCodingProblem(String role, String difficulty, String topic) {
        log.debug("Generating coding problem for role: {}, difficulty: {}, topic: {}", role, difficulty, topic);

        String responseJson = """
            {
                "title": "Two Sum Problem",
                "description": "Given an array of integers nums and an integer target, return indices of the two numbers that add up to target.",
                "examples": [
                    {
                        "input": "nums = [2,7,11,15], target = 9",
                        "output": "[0,1]",
                        "explanation": "Because nums[0] + nums[1] == 9"
                    }
                ],
                "constraints": ["2 <= nums.length <= 10^4", "-10^9 <= nums[i] <= 10^9", "-10^9 <= target <= 10^9"],
                "starterCode": "class Solution {\\n    public int[] twoSum(int[] nums, int target) {\\n        \\n    }\\n}",
                "language": "java",
                "expectedComplexity": {
                    "time": "O(n)",
                    "space": "O(n)"
                }
            }
            """;

        return AIResponse.builder()
                .content(responseJson)
                .success(true)
                .metadata(Map.of("type", "coding", "role", role, "difficulty", difficulty))
                .processingTimeMs(150L)
                .build();
    }

    @Override
    public AIResponse generateFollowUpQuestion(String role, String difficulty, String originalQuestion, String candidateAnswer) {
        log.debug("Generating follow-up question for role: {}, difficulty: {}", role, difficulty);

        String responseJson = String.format(
                "{\"question\":\"Can you elaborate more on your answer regarding %s?\",\"context\":\"Following up to test deeper understanding\",\"difficulty\":3,\"topic\":\"%s\",\"skillsAssessed\":[\"Critical Thinking\",\"Deep Understanding\"]}",
                originalQuestion, role
        );

        return AIResponse.builder()
                .content(responseJson)
                .success(true)
                .metadata(Map.of("type", "followup", "role", role, "difficulty", difficulty))
                .processingTimeMs(70L)
                .build();
    }

    @Override
    public AIResponse evaluateAnswer(Question question, Answer answer, InterviewSession session) {
        log.debug("Evaluating answer for question: {}", question.getId());

        String feedback;
        double score = 0.0;
        boolean isCorrect = false;

        if (QuestionType.MCQ.name().equals(question.getQuestionType())) {
            if (answer.getSelectedOptionIndex() != null &&
                    answer.getSelectedOptionIndex().equals(question.getCorrectOptionIndex())) {
                isCorrect = true;
                score = 1.0;
                feedback = "Correct! Good understanding of the concept.";
            } else {
                isCorrect = false;
                score = 0.0;
                feedback = "Incorrect. The correct answer was option " + (question.getCorrectOptionIndex() + 1);
            }
        } else {
            isCorrect = true;
            score = 0.7;
            feedback = "Good answer. You demonstrated understanding of the topic. Consider providing more specific examples next time.";
        }

        String responseJson = String.format(
                "{\"score\":%.2f,\"isCorrect\":%b,\"feedback\":\"%s\",\"strengths\":[\"Good structure\",\"Clear explanation\"],\"weaknesses\":[\"Could add more examples\"],\"suggestions\":\"Practice more concrete examples\"}",
                score * 100, isCorrect, feedback
        );

        return AIResponse.builder()
                .content(responseJson)
                .success(true)
                .metadata(Map.of("type", "evaluation", "questionId", question.getId()))
                .processingTimeMs(120L)
                .build();
    }

    @Override
    public AIResponse evaluateCodingSolution(String problem, String solution, String language, String role, String difficulty) {
        log.debug("Evaluating coding solution for role: {}, language: {}", role, language);

        String responseJson = """
            {
                "score": 85,
                "isCorrect": true,
                "feedback": "Good solution! The algorithm is correct and efficient.",
                "strengths": ["Correct algorithm", "Good time complexity", "Clean code"],
                "weaknesses": ["Could add more comments", "Consider edge cases"],
                "timeComplexity": "O(n)",
                "spaceComplexity": "O(n)",
                "suggestions": "Add input validation for edge cases and more comments for clarity."
            }
            """;

        return AIResponse.builder()
                .content(responseJson)
                .success(true)
                .metadata(Map.of("type", "coding-evaluation", "role", role))
                .processingTimeMs(200L)
                .build();
    }

    @Override
    public AIResponse chat(String userMessage, List<AIRequest> conversationHistory) {
        log.debug("Processing chat message: {}", userMessage);

        String response = String.format("I understand your message: '%s'. Let me help you with that.", userMessage);

        return AIResponse.builder()
                .content(response)
                .success(true)
                .metadata(Map.of("type", "chat"))
                .processingTimeMs(50L)
                .build();
    }

    @Override
    public AIResponse getRecommendations(String role, String difficulty, String performanceData) {
        log.debug("Generating recommendations for role: {}, difficulty: {}", role, difficulty);

        String response = String.format("""
            Based on your performance in %s at %s level:
            - Strengths: Good conceptual understanding
            - Areas to improve: Practice more practical problems
            - Recommended next topics: Advanced %s concepts
            - Suggested study time: 2-3 hours per week
            """, role, difficulty, role);

        return AIResponse.builder()
                .content(response)
                .success(true)
                .metadata(Map.of("type", "recommendations", "role", role))
                .processingTimeMs(100L)
                .build();
    }

    @Override
    public AIResponse evaluateSession(InterviewSession session, List<Question> questions, List<Answer> answers) {
        log.debug("Generating mock evaluation for session: {}", session.getSessionId());

        String feedbackSummary = String.format("### Overall Assessment\n\nThe candidate demonstrated a good level of familiarity with %s concepts at the %s level.\n\n### Technical Competence\n\n- Strong understanding of core topics.\n- Answered %d out of %d questions.\n\n### Areas for Improvement\n\n- Deepen practical implementation knowledge.\n- Focus on edge case resolution.",
                session.getRole(), session.getDifficulty(), session.getAnsweredQuestions(), session.getTotalQuestions());

        String responseJson = String.format("{\"score\":75.0,\"communicationScore\":80.0,\"clarityScore\":70.0,\"correctnessScore\":75.0,\"feedbackSummary\":\"%s\",\"strengths\":[\"Clear communication\",\"Structured approach\"],\"weaknesses\":[\"Lack of practical implementation details\"],\"suggestions\":[\"Practice coding daily\",\"Read design pattern books\"]}",
                feedbackSummary.replace("\n", "\\n").replace("\"", "\\\""));

        return AIResponse.builder()
                .content(responseJson)
                .success(true)
                .metadata(Map.of("type", "session-evaluation", "sessionId", session.getSessionId()))
                .processingTimeMs(100L)
                .build();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
