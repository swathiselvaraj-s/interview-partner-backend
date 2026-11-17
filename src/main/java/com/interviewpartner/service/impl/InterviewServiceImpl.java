package com.interviewpartner.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpartner.ai.WaveAIService;
import com.interviewpartner.ai.dto.AIResponse;
import com.interviewpartner.dto.request.InterviewAnswerRequest;
import com.interviewpartner.dto.request.InterviewEndRequest;
import com.interviewpartner.dto.request.InterviewStartRequest;
import com.interviewpartner.dto.response.AnswerResponse;
import com.interviewpartner.dto.response.InterviewSessionResponse;
import com.interviewpartner.dto.response.InterviewSummaryResponse;
import com.interviewpartner.dto.response.QuestionResponse;
import com.interviewpartner.dto.response.QuestionWithAnswerResponse;
import com.interviewpartner.entity.Answer;
import com.interviewpartner.entity.InterviewSession;
import com.interviewpartner.entity.Question;
import com.interviewpartner.entity.User;
import com.interviewpartner.exception.BadRequestException;
import com.interviewpartner.exception.ResourceNotFoundException;
import com.interviewpartner.interview.enums.DifficultyLevel;
import com.interviewpartner.interview.enums.InterviewMode;
import com.interviewpartner.interview.enums.QuestionType;
import com.interviewpartner.interview.enums.SessionStatus;
import com.interviewpartner.repository.AnswerRepository;
import com.interviewpartner.repository.InterviewSessionRepository;
import com.interviewpartner.repository.QuestionRepository;
import com.interviewpartner.repository.UserRepository;
import com.interviewpartner.service.InterviewService;
import com.interviewpartner.service.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;
    private final WaveAIService waveAIService;

    @Override
    @Transactional
    public InterviewSessionResponse startInterview(String email, InterviewStartRequest request) {
        log.debug("Starting new interview for user: {}", email);

        User user = getUserByEmail(email);
        validateStartRequest(request);

        String sessionId = UUID.randomUUID().toString();

        // Create session
        InterviewSession session = InterviewSession.builder()
                .sessionId(sessionId)
                .user(user)
                .mode(request.getMode())
                .role(request.getRole())
                .difficulty(request.getDifficulty())
                .timeLimit(request.getTimeLimit())
                .totalQuestions(0)
                .answeredQuestions(0)
                .correctAnswers(0)
                .score(0.0)
                .communicationScore(0.0)
                .clarityScore(0.0)
                .correctnessScore(0.0)
                .status(SessionStatus.IN_PROGRESS.name())
                .startedAt(LocalDateTime.now())
                .questions(new ArrayList<>())
                .build();

        // Set topics if provided (AFTER session creation)
        if (request.getTopics() != null && !request.getTopics().isEmpty()) {
            try {
                session.setSelectedTopics(objectMapper.writeValueAsString(request.getTopics()));
            } catch (JsonProcessingException e) {
                log.error("Error serializing topics: {}", e.getMessage());
            }
        }

        int numQuestions = request.getNumberOfQuestions() != null ?
                request.getNumberOfQuestions() : getDefaultQuestionsCount(request.getMode());
        session.setTotalQuestions(numQuestions);

        InterviewSession savedSession = sessionRepository.save(session);
        log.info("Interview session created: {} for user: {}", sessionId, email);

        generateAndAddQuestion(savedSession, 1);

        return mapToSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public QuestionResponse getNextQuestion(String sessionId) {
        log.debug("Getting next question for session: {}", sessionId);

        InterviewSession session = getActiveSession(sessionId);

        // Check if there is already an existing unanswered question (such as an injected follow-up question)
        List<Question> existingQuestions = questionRepository.findBySessionIdOrderBySequenceNumberAsc(session.getId());
        for (Question q : existingQuestions) {
            if (answerRepository.findByQuestionId(q.getId()).isEmpty()) {
                log.info("Returning pending question #{} (ID: {}) for session: {}", q.getSequenceNumber(), q.getId(), sessionId);
                return mapToQuestionResponse(q);
            }
        }

        int nextSequence = existingQuestions.size() + 1;
        if (nextSequence > session.getTotalQuestions()) {
            throw new BadRequestException("All questions have been answered. End the interview.");
        }

        return generateAndAddQuestion(session, nextSequence);
    }

    @Override
    @Transactional
    public QuestionResponse submitAnswer(String sessionId, InterviewAnswerRequest request) {
        log.debug("Submitting answer for session: {}", sessionId);

        InterviewSession session = getActiveSession(sessionId);

        Question currentQuestion = questionRepository
                .findBySessionIdOrderBySequenceNumberAsc(session.getId())
                .stream()
                .filter(q -> q.getAnswers().isEmpty())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No pending question to answer"));

        Answer answer = Answer.builder()
                .question(currentQuestion)
                .session(session)
                .answerText(request.getAnswerText())
                .selectedOptionIndex(request.getSelectedOptionIndex())
                .timeSpentSeconds(request.getTimeSpentSeconds())
                .answeredAt(LocalDateTime.now())
                .build();

        evaluateAnswer(answer, currentQuestion);

        answerRepository.save(answer);
        log.debug("Answer saved for question: {} in session: {}", currentQuestion.getId(), sessionId);

        session.setAnsweredQuestions(session.getAnsweredQuestions() + 1);
        if (answer.getIsCorrect() != null && answer.getIsCorrect()) {
            session.setCorrectAnswers(session.getCorrectAnswers() + 1);
        }
        sessionRepository.save(session);

        analyticsService.updateSessionAnalytics(session);

        return mapToQuestionResponse(currentQuestion);
    }

    @Override
    public InterviewSessionResponse getSessionStatus(String sessionId) {
        InterviewSession session = getSessionBySessionId(sessionId);
        return mapToSessionResponse(session);
    }

    @Override
    @Transactional
    public InterviewSummaryResponse endInterview(String sessionId, InterviewEndRequest request) {
        log.debug("Ending interview session: {}", sessionId);

        InterviewSession session = getSessionBySessionId(sessionId);

        if (request.isAbandon()) {
            session.setStatus(SessionStatus.ABANDONED.name());
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
            throw new BadRequestException("Interview abandoned");
        }

        calculateFinalScores(session);
        session.setStatus(SessionStatus.COMPLETED.name());
        session.setCompletedAt(LocalDateTime.now());

        List<Question> questions = questionRepository.findBySessionIdOrderBySequenceNumberAsc(session.getId());
        List<Answer> answers = answerRepository.findBySessionId(session.getId());

        AIResponse aiResponse = waveAIService.evaluateSession(session, questions, answers);
        String finalFeedbackJson = aiResponse.isSuccess() ? aiResponse.getContent() : generateFeedbackSummary(session);

        try {
            JsonNode rootNode = objectMapper.readTree(finalFeedbackJson.trim().startsWith("{") ? finalFeedbackJson : "{}");
            com.fasterxml.jackson.databind.node.ObjectNode root = rootNode.isObject() 
                    ? (com.fasterxml.jackson.databind.node.ObjectNode) rootNode 
                    : objectMapper.createObjectNode();

            if (root.has("score")) session.setScore(root.get("score").asDouble());
            if (root.has("communicationScore")) session.setCommunicationScore(root.get("communicationScore").asDouble());
            if (root.has("clarityScore")) session.setClarityScore(root.get("clarityScore").asDouble());
            if (root.has("correctnessScore")) session.setCorrectnessScore(root.get("correctnessScore").asDouble());

            // Save request telemetry if provided
            if (request.getEyeContactScore() != null) root.put("eyeContactScore", request.getEyeContactScore());
            if (request.getEyeContactWarnings() != null) root.put("eyeContactWarnings", request.getEyeContactWarnings());
            if (request.getTimeLookingAwaySeconds() != null) root.put("timeLookingAwaySeconds", request.getTimeLookingAwaySeconds());
            if (request.getSpeakingPaceWpm() != null) root.put("speakingPaceWpm", request.getSpeakingPaceWpm());
            if (request.getTotalSpeechDurationSeconds() != null) root.put("totalSpeechDurationSeconds", request.getTotalSpeechDurationSeconds());
            if (request.getAverageResponseLatencySeconds() != null) root.put("averageResponseLatencySeconds", request.getAverageResponseLatencySeconds());
            if (request.getFillerWordCount() != null) root.put("fillerWordCount", request.getFillerWordCount());

            session.setFeedbackSummary(objectMapper.writeValueAsString(root));
        } catch (Exception e) {
            log.error("Failed to parse AI evaluation JSON: {}", e.getMessage());
            session.setFeedbackSummary(finalFeedbackJson);
        }

        InterviewSession savedSession = sessionRepository.save(session);
        log.info("Interview session completed: {}", sessionId);

        analyticsService.finalizeSessionAnalytics(savedSession);

        return generateSummaryResponse(savedSession);
    }

    @Override
    public Page<InterviewSessionResponse> getSessionHistory(String email, int page, int size) {
        User user = getUserByEmail(email);
        Pageable pageable = PageRequest.of(page, size);

        return sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::mapToSessionResponse);
    }

    @Override
    public Page<InterviewSessionResponse> getSessionHistoryByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToSessionResponse);
    }

    @Override
    public InterviewSessionResponse getSessionDetails(String sessionId) {
        InterviewSession session = getSessionBySessionId(sessionId);
        return mapToSessionResponse(session);
    }

    @Override
    public List<QuestionResponse> getAllQuestions(String sessionId) {
        InterviewSession session = getSessionBySessionId(sessionId);
        return questionRepository.findBySessionIdOrderBySequenceNumberAsc(session.getId())
                .stream()
                .map(this::mapToQuestionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void abandonSession(String sessionId) {
        InterviewSession session = getSessionBySessionId(sessionId);
        session.setStatus(SessionStatus.ABANDONED.name());
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("Session abandoned: {}", sessionId);
    }

    @Override
    public boolean isSessionActive(String sessionId) {
        try {
            InterviewSession session = getSessionBySessionId(sessionId);
            return SessionStatus.IN_PROGRESS.name().equals(session.getStatus());
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    // ============ Private Helper Methods ============

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private InterviewSession getActiveSession(String sessionId) {
        InterviewSession session = getSessionBySessionId(sessionId);
        if (!SessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            throw new BadRequestException("Session is not active. Current status: " + session.getStatus());
        }
        return session;
    }

    private InterviewSession getSessionBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));
    }

    private void validateStartRequest(InterviewStartRequest request) {
        try {
            InterviewMode.valueOf(request.getMode());
            DifficultyLevel.valueOf(request.getDifficulty());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid mode or difficulty level");
        }
    }

    private int getDefaultQuestionsCount(String mode) {
        return switch (mode) {
            case "MCQ" -> 10;
            case "CODING" -> 3;
            case "VOICE" -> 8;
            case "CUSTOM" -> 5;
            default -> 7;
        };
    }

    /**
     * Generate next question with AI - with follow-up logic
     */
    private QuestionResponse generateAndAddQuestion(InterviewSession session, int sequence) {
        String role = session.getRole();
        String difficulty = session.getDifficulty();

        String topicFocus = getTopicFocus(session);
        List<Question> previousQuestions = questionRepository
                .findBySessionIdOrderBySequenceNumberAsc(session.getId());
        String conversationContext = buildConversationContext(previousQuestions);
        List<String> previousQuestionTexts = previousQuestions.stream()
                .map(Question::getQuestionText)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        AIResponse aiResponse;

        if ("MCQ".equals(session.getMode())) {
            aiResponse = waveAIService.generateMCQ(role, difficulty, topicFocus);
        } else if ("CODING".equals(session.getMode())) {
            aiResponse = waveAIService.generateCodingProblem(role, difficulty, "algorithms");
        } else {
            aiResponse = waveAIService.generateInterviewQuestion(
                    role,
                    difficulty,
                    sequence,
                    topicFocus,
                    conversationContext,
                    previousQuestionTexts
            );
        }

        if (!aiResponse.isSuccess()) {
            log.error("AI generation failed: {}", aiResponse.getErrorMessage());
            return createFallbackQuestion(session, sequence);
        }

        Question question = parseAIResponseToQuestion(aiResponse, session, sequence);
        Question savedQuestion = questionRepository.save(question);
        return mapToQuestionResponse(savedQuestion);
    }

    private String getTopicFocus(InterviewSession session) {
        if (session.getSelectedTopics() == null || session.getSelectedTopics().isBlank()) {
            return "core concepts";
        }

        try {
            List<String> topics = objectMapper.readValue(session.getSelectedTopics(), new TypeReference<List<String>>() {});
            if (topics == null || topics.isEmpty()) {
                return "core concepts";
            }
            return String.join(", ", topics);
        } catch (Exception e) {
            log.warn("Could not parse selected topics for session {}: {}", session.getSessionId(), e.getMessage());
            return "core concepts";
        }
    }

    private String buildConversationContext(List<Question> previousQuestions) {
        if (previousQuestions == null || previousQuestions.isEmpty()) {
            return "No previous Q&A yet.";
        }

        int start = Math.max(0, previousQuestions.size() - 6);
        StringBuilder context = new StringBuilder();

        for (Question question : previousQuestions.subList(start, previousQuestions.size())) {
            context.append("Q")
                    .append(question.getSequenceNumber())
                    .append(": ")
                    .append(question.getQuestionText())
                    .append("\n");

            answerRepository.findByQuestionId(question.getId()).ifPresent(answer ->
                    context.append("A")
                            .append(question.getSequenceNumber())
                            .append(": ")
                            .append(answer.getAnswerText())
                            .append("\n")
            );
        }

        return context.toString();
    }

    /**
     * Parse AI response to Question entity
     */
    private Question parseAIResponseToQuestion(AIResponse aiResponse, InterviewSession session, int sequence) {
        try {
            JsonNode json = objectMapper.readTree(aiResponse.getContent());

            if ("CODING".equals(session.getMode())) {
                String title = json.has("title") ? json.get("title").asText() : "Coding Problem";
                String description = json.has("description") ? json.get("description").asText() : "";
                String topic = json.has("topic") ? json.get("topic").asText() : "Coding";
                int difficulty = parseDifficulty(json, 3);
                String metadataJson = objectMapper.writeValueAsString(json);

                return Question.builder()
                        .session(session)
                        .questionText(title + (description.isBlank() ? "" : "\n\n" + description))
                        .questionType(QuestionType.CODING.name())
                        .category(topic)
                        .difficulty(difficulty)
                        .sequenceNumber(sequence)
                        .expectedAnswer(metadataJson)
                        .build();
            }

            String questionText = json.has("question") ? json.get("question").asText() : "Question not available";
            String topic = json.has("topic") ? json.get("topic").asText() : "General";
            int difficulty = json.has("difficulty") ? json.get("difficulty").asInt() : 2;
            String context = json.has("context") ? json.get("context").asText() : "";

            if (json.has("options")) {
                JsonNode optionsArray = json.get("options");
                List<String> options = new ArrayList<>();
                for (JsonNode opt : optionsArray) {
                    options.add(opt.asText());
                }
                int correctIndex = json.has("correctIndex") ? json.get("correctIndex").asInt() : 0;
                String explanation = json.has("explanation") ? json.get("explanation").asText() : "";

                return Question.builder()
                        .session(session)
                        .questionText(questionText)
                        .questionType(QuestionType.MCQ.name())
                        .category(topic)
                        .difficulty(difficulty)
                        .sequenceNumber(sequence)
                        .options(objectMapper.writeValueAsString(options))
                        .correctOptionIndex(correctIndex)
                        .expectedAnswer(explanation)
                        .build();
            }

            return Question.builder()
                    .session(session)
                    .questionText(questionText)
                    .questionType(QuestionType.TEXT.name())
                    .category(topic)
                    .difficulty(difficulty)
                    .sequenceNumber(sequence)
                    .expectedAnswer(context)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            return createFallbackQuestionEntity(session, sequence);
        }
    }

    private int parseDifficulty(JsonNode json, int defaultValue) {
        if (!json.has("difficulty")) {
            return defaultValue;
        }
        JsonNode difficultyNode = json.get("difficulty");
        if (difficultyNode.isInt()) {
            return difficultyNode.asInt();
        }
        if (difficultyNode.isTextual()) {
            String value = difficultyNode.asText().toUpperCase(Locale.ROOT);
            return switch (value) {
                case "BEGINNER", "EASY" -> 1;
                case "ADVANCED", "HARD" -> 5;
                default -> 3;
            };
        }
        return defaultValue;
    }

    /**
     * Enhanced evaluation with AI
     */
    private void evaluateAnswer(Answer answer, Question question) {
        InterviewSession session = answer.getSession();

        if ("MCQ".equals(question.getQuestionType()) && question.getCorrectOptionIndex() != null) {
            boolean isCorrect = Objects.equals(answer.getSelectedOptionIndex(), question.getCorrectOptionIndex());
            answer.setIsCorrect(isCorrect);
            answer.setScore(isCorrect ? 1.0 : 0.0);
            answer.setCorrectnessScore(isCorrect ? 1.0 : 0.0);
            answer.setClarityScore(1.0);
            answer.setCommunicationScore(1.0);
            answer.setAiFeedback(isCorrect ? "Correct answer!" : "Incorrect. The correct option was " + (char)('A' + question.getCorrectOptionIndex()) + ".");
            return;
        }

        AIResponse aiResponse = waveAIService.evaluateAnswer(question, answer, session);

        if (!aiResponse.isSuccess()) {
            log.error("AI evaluation failed: {}", aiResponse.getErrorMessage());
            answer.setScore(0.5);
            answer.setIsCorrect(false);
            answer.setAiFeedback("Could not evaluate answer. Please review your answer.");
            return;
        }

        try {
            JsonNode json = objectMapper.readTree(aiResponse.getContent());

            double score = json.has("score") ? json.get("score").asDouble() : 50.0;
            boolean isCorrect = json.has("isCorrect") && json.get("isCorrect").asBoolean();
            String feedback = json.has("feedback") ? json.get("feedback").asText() : "No feedback available.";
            double communicationScore = json.has("communicationScore") ? json.get("communicationScore").asDouble() : 50.0;
            double clarityScore = json.has("clarityScore") ? json.get("clarityScore").asDouble() : 50.0;
            double correctnessScore = json.has("correctnessScore") ? json.get("correctnessScore").asDouble() : 50.0;

            answer.setScore(score / 100.0);
            answer.setIsCorrect(isCorrect);
            answer.setAiFeedback(feedback);
            answer.setCommunicationScore(communicationScore / 100.0);
            answer.setClarityScore(clarityScore / 100.0);
            answer.setCorrectnessScore(correctnessScore / 100.0);

            if (json.has("followUpNeeded") && json.get("followUpNeeded").asBoolean()) {
                if (json.has("followUpQuestion")) {
                    String followUpQuestion = json.get("followUpQuestion").asText();
                    if (followUpQuestion != null && !followUpQuestion.trim().isEmpty()) {
                        log.info("AI recommends follow-up question: {}", followUpQuestion);
                        int nextSeq = question.getSequenceNumber() + 1;

                        Question followUp = Question.builder()
                                .session(session)
                                .questionText(followUpQuestion.trim())
                                .questionType(QuestionType.TEXT.name())
                                .category(question.getCategory() != null ? question.getCategory() : "Follow-up")
                                .difficulty(question.getDifficulty())
                                .sequenceNumber(nextSeq)
                                .expectedAnswer("Follow-up elaboration based on previous answer")
                                .build();

                        questionRepository.save(followUp);
                        if (session.getQuestions() != null) {
                            session.getQuestions().add(followUp);
                        }
                        session.setTotalQuestions(session.getTotalQuestions() + 1);
                        sessionRepository.save(session);
                        log.info("Injected follow-up question #{} for session: {}", nextSeq, session.getSessionId());
                    }
                }
            }

            log.debug("Answer evaluated - Score: {}, Correct: {}, Communication: {}, Clarity: {}",
                    score, isCorrect, communicationScore, clarityScore);

        } catch (Exception e) {
            log.error("Error parsing AI evaluation: {}", e.getMessage());
            answer.setScore(0.5);
            answer.setIsCorrect(false);
            answer.setAiFeedback("AI evaluation failed. Please review your answer.");
        }
    }

    /**
     * Generate fallback question if AI fails
     */
    private QuestionResponse createFallbackQuestion(InterviewSession session, int sequence) {
        List<String> fallbackQuestions = Arrays.asList(
                "Explain the key concepts of " + session.getRole() + " development.",
                "What are the main challenges in " + session.getRole() + " programming?",
                "Describe your experience with " + session.getRole() + " technologies.",
                "How do you approach problem-solving in " + session.getRole() + "?",
                "What are the best practices for " + session.getRole() + " development?"
        );

        String questionText = fallbackQuestions.get(sequence % fallbackQuestions.size());

        Question question = Question.builder()
                .session(session)
                .questionText(questionText)
                .questionType(QuestionType.TEXT.name())
                .category("General")
                .difficulty(2)
                .sequenceNumber(sequence)
                .build();

        Question savedQuestion = questionRepository.save(question);
        return mapToQuestionResponse(savedQuestion);
    }

    private Question createFallbackQuestionEntity(InterviewSession session, int sequence) {
        List<String> fallbackQuestions = Arrays.asList(
                "Explain the key concepts of " + session.getRole() + " development.",
                "What are the main challenges in " + session.getRole() + " programming?",
                "Describe your experience with " + session.getRole() + " technologies.",
                "How do you approach problem-solving in " + session.getRole() + "?",
                "What are the best practices for " + session.getRole() + " development?"
        );

        String questionText = fallbackQuestions.get(sequence % fallbackQuestions.size());

        return Question.builder()
                .session(session)
                .questionText(questionText)
                .questionType(QuestionType.TEXT.name())
                .category("General")
                .difficulty(2)
                .sequenceNumber(sequence)
                .build();
    }

    private String generateQuestionText(InterviewSession session, int sequence) {
        return String.format("Question %d for %s interview at %s level. [AI will generate actual question]",
                sequence, session.getRole(), session.getDifficulty());
    }

    private String getCategoryForRole(String role) {
        return switch (role) {
            case "JAVA" -> "Java Core";
            case "FRONTEND" -> "JavaScript/React";
            case "BACKEND" -> "System Design";
            case "DSA" -> "Data Structures";
            default -> "General";
        };
    }

    private int getDifficultyLevel(String difficulty) {
        return switch (difficulty) {
            case "BEGINNER" -> 1;
            case "ADVANCED" -> 3;
            default -> 2;
        };
    }

    private void calculateFinalScores(InterviewSession session) {
        List<Answer> answers = answerRepository.findBySessionId(session.getId());

        if (answers.isEmpty()) {
            return;
        }

        long correctCount = answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        session.setCorrectAnswers((int) correctCount);
        session.setCorrectnessScore((double) correctCount / answers.size() * 100);

        double avgScore = answers.stream()
                .mapToDouble(a -> a.getScore() != null ? a.getScore() : 0.0)
                .average()
                .orElse(0.0);
        session.setScore(avgScore * 100);

        double avgCommunication = answers.stream()
                .mapToDouble(a -> a.getCommunicationScore() != null ? a.getCommunicationScore() : 0.0)
                .average()
                .orElse(0.0);
        session.setCommunicationScore(avgCommunication * 100);

        double avgClarity = answers.stream()
                .mapToDouble(a -> a.getClarityScore() != null ? a.getClarityScore() : 0.0)
                .average()
                .orElse(0.0);
        session.setClarityScore(avgClarity * 100);
    }

    private String generateFeedbackSummary(InterviewSession session) {
        return String.format("""
                Interview Summary:
                - Total Questions: %d
                - Correct Answers: %d
                - Overall Score: %.2f%%
                - Communication: %.2f%%
                - Clarity: %.2f%%
                - Correctness: %.2f%%
                
                Strengths: Good understanding of core concepts
                Areas for Improvement: Need more practice with practical problems
                """,
                session.getTotalQuestions(),
                session.getCorrectAnswers(),
                session.getScore(),
                session.getCommunicationScore(),
                session.getClarityScore(),
                session.getCorrectnessScore()
        );
    }

    private InterviewSummaryResponse generateSummaryResponse(InterviewSession session) {
        List<Question> questions = questionRepository.findBySessionIdOrderBySequenceNumberAsc(session.getId());
        List<QuestionWithAnswerResponse> questionDetails = questions.stream()
                .map(q -> {
                    Optional<Answer> answerOpt = answerRepository.findByQuestionId(q.getId());
                    return QuestionWithAnswerResponse.builder()
                            .question(mapToQuestionResponse(q))
                            .answer(answerOpt.map(this::mapToAnswerResponse).orElse(null))
                            .hasAnswered(answerOpt.isPresent())
                            .build();
                })
                .collect(Collectors.toList());

        Map<String, Double> categoryScores = new HashMap<>();
        String feedbackSummaryText = session.getFeedbackSummary();
        List<String> strengthsList = new ArrayList<>();
        List<String> weaknessesList = new ArrayList<>();
        String suggestionsText = "Practice more coding problems and system design questions";

        Double eyeContactScore = null;
        Integer eyeContactWarnings = null;
        Integer timeLookingAwaySeconds = null;
        Double speakingPaceWpm = null;
        Double totalSpeechDurationSeconds = null;
        Double averageResponseLatencySeconds = null;
        Integer fillerWordCount = null;

        if (session.getFeedbackSummary() != null && session.getFeedbackSummary().trim().startsWith("{")) {
            try {
                JsonNode json = objectMapper.readTree(session.getFeedbackSummary());
                if (json.has("feedbackSummary")) {
                    feedbackSummaryText = json.get("feedbackSummary").asText();
                }
                if (json.has("strengths")) {
                    for (JsonNode node : json.get("strengths")) {
                        strengthsList.add(node.asText());
                    }
                }
                if (json.has("weaknesses")) {
                    for (JsonNode node : json.get("weaknesses")) {
                        weaknessesList.add(node.asText());
                    }
                }
                if (json.has("suggestions")) {
                    List<String> suggestionList = new ArrayList<>();
                    for (JsonNode node : json.get("suggestions")) {
                        suggestionList.add(node.asText());
                    }
                    if (!suggestionList.isEmpty()) {
                        suggestionsText = String.join("\n", suggestionList);
                    }
                }
                
                // Telemetry values mapping
                if (json.has("eyeContactScore")) eyeContactScore = json.get("eyeContactScore").asDouble();
                if (json.has("eyeContactWarnings")) eyeContactWarnings = json.get("eyeContactWarnings").asInt();
                if (json.has("timeLookingAwaySeconds")) timeLookingAwaySeconds = json.get("timeLookingAwaySeconds").asInt();
                if (json.has("speakingPaceWpm")) speakingPaceWpm = json.get("speakingPaceWpm").asDouble();
                if (json.has("totalSpeechDurationSeconds")) totalSpeechDurationSeconds = json.get("totalSpeechDurationSeconds").asDouble();
                if (json.has("averageResponseLatencySeconds")) averageResponseLatencySeconds = json.get("averageResponseLatencySeconds").asDouble();
                if (json.has("fillerWordCount")) fillerWordCount = json.get("fillerWordCount").asInt();
            } catch (Exception e) {
                log.warn("Failed to parse feedbackSummary JSON in generateSummaryResponse: {}", e.getMessage());
            }
        }

        if (strengthsList.isEmpty()) {
            strengthsList = List.of("Good analytical skills", "Clear communication");
        }
        if (weaknessesList.isEmpty()) {
            weaknessesList = List.of("Need more practice", "Work on complex scenarios");
        }

        return InterviewSummaryResponse.builder()
                .sessionId(session.getSessionId())
                .mode(session.getMode())
                .role(session.getRole())
                .difficulty(session.getDifficulty())
                .totalQuestions(session.getTotalQuestions())
                .answeredQuestions(session.getAnsweredQuestions())
                .correctAnswers(session.getCorrectAnswers())
                .overallScore(session.getScore())
                .categoryScores(categoryScores)
                .feedbackSummary(feedbackSummaryText)
                .strengths(strengthsList)
                .weaknesses(weaknessesList)
                .improvementSuggestions(suggestionsText)
                .questionDetails(questionDetails)
                .eyeContactScore(eyeContactScore)
                .eyeContactWarnings(eyeContactWarnings)
                .timeLookingAwaySeconds(timeLookingAwaySeconds)
                .speakingPaceWpm(speakingPaceWpm)
                .totalSpeechDurationSeconds(totalSpeechDurationSeconds)
                .averageResponseLatencySeconds(averageResponseLatencySeconds)
                .fillerWordCount(fillerWordCount)
                .build();
    }

    // ============ Mapping Methods ============

    private InterviewSessionResponse mapToSessionResponse(InterviewSession session) {
        List<QuestionResponse> questionResponses = questionRepository
                .findBySessionIdOrderBySequenceNumberAsc(session.getId())
                .stream()
                .map(this::mapToQuestionResponse)
                .collect(Collectors.toList());

        return InterviewSessionResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .mode(session.getMode())
                .role(session.getRole())
                .difficulty(session.getDifficulty())
                .timeLimit(session.getTimeLimit())
                .totalQuestions(session.getTotalQuestions())
                .answeredQuestions(session.getAnsweredQuestions())
                .correctAnswers(session.getCorrectAnswers())
                .score(session.getScore())
                .communicationScore(session.getCommunicationScore())
                .clarityScore(session.getClarityScore())
                .correctnessScore(session.getCorrectnessScore())
                .status(session.getStatus())
                .feedbackSummary(session.getFeedbackSummary())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .createdAt(session.getCreatedAt())
                .questions(questionResponses)
                .build();
    }

    private QuestionResponse mapToQuestionResponse(Question question) {
        List<String> options = null;
        if (question.getOptions() != null) {
            try {
                options = objectMapper.readValue(question.getOptions(),
                        new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                log.error("Error parsing options for question: {}", question.getId(), e);
            }
        }

        CodingQuestionMetadata codingMetadata = parseCodingQuestionMetadata(question);

        List<AnswerResponse> answerResponses = answerRepository.findByQuestionId(question.getId())
                .map(this::mapToAnswerResponse)
                .map(List::of)
                .orElse(List.of());

        return QuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .sequenceNumber(question.getSequenceNumber())
                .options(options)
                .title(codingMetadata.title())
                .description(codingMetadata.description())
                .examples(codingMetadata.examples())
                .constraints(codingMetadata.constraints())
                .tags(codingMetadata.tags())
                .starterCode(codingMetadata.starterCode())
                .language(codingMetadata.language())
                .expectedComplexity(codingMetadata.expectedComplexity())
                .publicTests(codingMetadata.publicTests())
                .hiddenTests(codingMetadata.hiddenTests())
                .answers(answerResponses)
                .createdAt(question.getCreatedAt())
                .build();
    }

    private CodingQuestionMetadata parseCodingQuestionMetadata(Question question) {
        if (!QuestionType.CODING.name().equals(question.getQuestionType()) || question.getExpectedAnswer() == null) {
            return CodingQuestionMetadata.empty();
        }

        try {
            JsonNode json = objectMapper.readTree(question.getExpectedAnswer());
            return new CodingQuestionMetadata(
                    textOrNull(json, "title"),
                    textOrNull(json, "description"),
                    readStringMapList(json.get("examples")),
                    readStringList(json.get("constraints")),
                    readStringList(json.get("tags")),
                    textOrNull(json, "starterCode"),
                    textOrNull(json, "language"),
                    readStringMap(json.get("expectedComplexity")),
                    readStringMapList(json.get("publicTests")),
                    readStringMapList(json.get("hiddenTests"))
            );
        } catch (Exception e) {
            log.warn("Could not parse coding metadata for question {}: {}", question.getId(), e.getMessage());
            return CodingQuestionMetadata.empty();
        }
    }

    private String textOrNull(JsonNode json, String fieldName) {
        return json.has(fieldName) && !json.get(fieldName).isNull() ? json.get(fieldName).asText() : null;
    }

    private List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    private Map<String, String> readStringMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> values.put(entry.getKey(), entry.getValue().asText()));
        return values;
    }

    private List<Map<String, String>> readStringMapList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return null;
        }
        List<Map<String, String>> values = new ArrayList<>();
        for (JsonNode item : node) {
            Map<String, String> mapped = readStringMap(item);
            if (mapped != null) {
                values.add(mapped);
            }
        }
        return values;
    }

    private record CodingQuestionMetadata(
            String title,
            String description,
            List<Map<String, String>> examples,
            List<String> constraints,
            List<String> tags,
            String starterCode,
            String language,
            Map<String, String> expectedComplexity,
            List<Map<String, String>> publicTests,
            List<Map<String, String>> hiddenTests
    ) {
        private static CodingQuestionMetadata empty() {
            return new CodingQuestionMetadata(null, null, null, null, null, null, null, null, null, null);
        }
    }

    private AnswerResponse mapToAnswerResponse(Answer answer) {
        return AnswerResponse.builder()
                .id(answer.getId())
                .answerText(answer.getAnswerText())
                .selectedOptionIndex(answer.getSelectedOptionIndex())
                .isCorrect(answer.getIsCorrect())
                .score(answer.getScore())
                .aiFeedback(answer.getAiFeedback())
                .timeSpentSeconds(answer.getTimeSpentSeconds())
                .communicationScore(answer.getCommunicationScore())
                .clarityScore(answer.getClarityScore())
                .correctnessScore(answer.getCorrectnessScore())
                .answeredAt(answer.getAnsweredAt())
                .build();
    }
}
