package com.interviewpartner.ai;

import com.interviewpartner.ai.dto.AIRequest;
import com.interviewpartner.ai.dto.AIResponse;
import com.interviewpartner.entity.Answer;
import com.interviewpartner.entity.InterviewSession;
import com.interviewpartner.entity.Question;

import java.util.List;

public interface WaveAIService {

    AIResponse generateMCQ(String role, String difficulty, String topic);

    AIResponse generateInterviewQuestion(
            String role,
            String difficulty,
            int questionNumber,
            String topicFocus,
            String conversationContext,
            List<String> previousQuestions
    );

    AIResponse generateCodingProblem(String role, String difficulty, String topic);

    AIResponse generateFollowUpQuestion(String role, String difficulty, String originalQuestion, String candidateAnswer);

    AIResponse evaluateAnswer(Question question, Answer answer, InterviewSession session);

    AIResponse evaluateCodingSolution(String problem, String solution, String language, String role, String difficulty);

    AIResponse chat(String userMessage, List<AIRequest> conversationHistory);

    AIResponse getRecommendations(String role, String difficulty, String performanceData);

    AIResponse evaluateSession(InterviewSession session, List<Question> questions, List<Answer> answers);

    boolean isAvailable();

    String getProviderName();
}
