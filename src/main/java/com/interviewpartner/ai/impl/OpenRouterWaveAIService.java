package com.interviewpartner.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.interviewpartner.ai.WaveAIService;
import com.interviewpartner.ai.dto.AIRequest;
import com.interviewpartner.ai.dto.AIResponse;
import com.interviewpartner.config.AIConfig;
import com.interviewpartner.entity.Answer;
import com.interviewpartner.entity.InterviewSession;
import com.interviewpartner.entity.Question;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class OpenRouterWaveAIService implements WaveAIService {

    private final AIConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouter.api.site-url:http://localhost:8080}")
    private String siteUrl;

    @Value("${openrouter.api.site-name:Interview Partner}")
    private String siteName;

    public OpenRouterWaveAIService(AIConfig aiConfig, ObjectMapper objectMapper) {
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public AIResponse generateMCQ(String role, String difficulty, String topic) {
        log.debug("Generating MCQ with OpenRouter for role: {}, difficulty: {}", role, difficulty);

        String systemPrompt = """
            You are WAVE, an expert AI interview assistant specializing in generating high-quality multiple-choice questions.
            Generate clear, accurate, and challenging MCQs with exactly 4 options.
            Only one option should be correct.
            Return ONLY valid JSON, no other text.
            """;

        String userPrompt = String.format("""
            Generate a multiple-choice question for a %s interview at %s level.
            
            Topic: %s
            Difficulty: %s (1-5, where 5 is most difficult)
            
            Requirements:
            - Question should test %s knowledge
            - Provide exactly 4 options
            - Clearly mark the correct answer index (0-3)
            - Include a brief explanation
            
            Return ONLY this JSON format:
            {
                "question": "The question text",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctIndex": 0,
                "explanation": "Why this is the correct answer",
                "topic": "Specific topic covered",
                "difficulty": 3
            }
            """, role, difficulty, topic, difficulty, role);

        return callOpenRouter(systemPrompt, userPrompt);
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
        log.debug("Generating interview question with OpenRouter for role: {}, difficulty: {}, question: {}",
                role, difficulty, questionNumber);

        String systemPrompt = """
            You are WAVE, an expert AI interviewer conducting a professional job interview.
            Your role is to assess the candidate's knowledge, communication skills, and problem-solving abilities.
            Ask relevant, challenging questions based on the role and difficulty level.
            Generate dynamic follow-up questions based on the full interview context.
            Never repeat any previous question or ask a near-duplicate.
            Ask one concise question at a time, like a real interviewer.
            Return ONLY valid JSON, no other text.
            """;

        String userPrompt;

        if (questionNumber == 1) {
            userPrompt = String.format("""
                Start an interview with a candidate for a %s position at %s level.
                This is question number %d - the first question.
                Topic focus: %s
                
                Requirements:
                - Ask an engaging opening question
                - Question should be relevant to %s role
                - Question should focus on the selected topic area when possible
                - Difficulty should match %s level
                - Include brief context
                
                Return ONLY this JSON format:
                {
                    "question": "The interview question",
                    "context": "Additional context or scenario",
                    "topic": "Topic being assessed",
                    "difficulty": 2,
                    "expectedSkills": ["Skill 1", "Skill 2"]
                }
                """, role, difficulty, questionNumber, topicFocus, role, difficulty);
        } else {
            userPrompt = String.format("""
                Based on the interview so far, generate the next best follow-up question.
                
                Role: %s
                Difficulty: %s
                Question Number: %d
                Topic focus: %s
                Previous questions to avoid: %s
                Recent conversation:
                %s
                
                Requirements:
                - Ask a follow-up question that probes deeper into the most recent answer
                - Address gaps, vague areas, or missed trade-offs from the candidate's answers
                - Keep the question aligned with role, topic focus, and difficulty
                - Do not repeat or paraphrase any previous question
                - Ask only one question
                
                Return ONLY this JSON format:
                {
                    "question": "The follow-up question",
                    "context": "Why this follow-up is relevant",
                    "topic": "Topic being probed",
                    "difficulty": 3,
                    "expectedSkills": ["Skill 1", "Skill 2"],
                    "followUpReason": "Why this follow-up was asked based on previous answer"
                }
                """, role, difficulty, questionNumber, topicFocus, previousQuestions, conversationContext);
        }

        return callOpenRouter(systemPrompt, userPrompt);
    }

    @Override
    public AIResponse generateCodingProblem(String role, String difficulty, String topic) {
        log.debug("Generating coding problem with OpenRouter for role: {}, difficulty: {}", role, difficulty);

        String systemPrompt = """
            You are WAVE, an expert coding interview assistant.
            Generate coding problems based on the role and difficulty level.
            Return ONLY valid JSON, no other text.
            """;

        String userPrompt = String.format("""
            Generate a coding problem for a %s interview at %s level.
            
            Topic: %s
            Difficulty: %d/5
            Language: Java
                
            Requirements:
            - Problem should test %s concepts
            - Include input/output examples
            - Specify constraints
            - Include tags for the main concepts tested
            - Include public tests and hidden tests suitable for Judge0 validation
            
            Return ONLY this JSON format:
            {
                "title": "Problem title",
                "description": "Problem description",
                "difficulty": 3,
                "tags": ["arrays", "hash map"],
                "examples": [
                    {
                        "input": "example input",
                        "output": "expected output",
                        "explanation": "why this is correct"
                    }
                ],
                "constraints": ["Constraint 1", "Constraint 2"],
                "starterCode": "class Solution {\\n    public void solve() {\\n        \\n    }\\n}",
                "language": "java",
                "expectedComplexity": {
                    "time": "O(n)",
                    "space": "O(n)"
                },
                "publicTests": [
                    {
                        "stdin": "input passed to the program",
                        "expectedOutput": "expected stdout"
                    }
                ],
                "hiddenTests": [
                    {
                        "stdin": "edge-case input passed to the program",
                        "expectedOutput": "expected stdout"
                    }
                ]
            }
            """, role, difficulty, topic, 3, role);

        return callOpenRouter(systemPrompt, userPrompt);
    }

    @Override
    public AIResponse generateFollowUpQuestion(String role, String difficulty, String originalQuestion, String candidateAnswer) {
        log.debug("Generating follow-up question with OpenRouter for role: {}", role);

        String systemPrompt = """
            You are WAVE, an expert AI interviewer.
            Based on the candidate's answer, generate a follow-up question that tests deeper understanding.
            Return ONLY valid JSON, no other text.
            """;

        String userPrompt = String.format("""
            Generate a follow-up question based on the candidate's answer.
            
            Role: %s
            Difficulty: %s
            Original Question: %s
            Candidate's Answer: %s
            
            Requirements:
            - Probe deeper into the topic
            - Address any gaps in the candidate's answer
            - Challenge the candidate's thinking
            - The follow-up should be personalized to the answer
            
            Return ONLY this JSON format:
            {
                "question": "The follow-up question",
                "context": "Why this follow-up is relevant",
                "difficulty": 3,
                "topic": "Topic being probed",
                "skillsAssessed": ["Critical Thinking", "Deep Understanding"],
                "reasonForFollowUp": "Explain why you're asking this based on the candidate's answer"
            }
            """, role, difficulty, originalQuestion, candidateAnswer);

        return callOpenRouter(systemPrompt, userPrompt);
    }

    @Override
    public AIResponse evaluateAnswer(Question question, Answer answer, InterviewSession session) {
        log.debug("Evaluating answer with OpenRouter for question: {}", question.getId());

        String systemPrompt = """
            You are WAVE, an expert AI interviewer evaluating candidate answers.
            Provide detailed, constructive feedback.
            Return ONLY valid JSON, no other text.
            """;

        String userPrompt = String.format("""
            Evaluate the candidate's answer.
            
            Question: %s
            Question Type: %s
            Candidate Answer: %s
            Role: %s
            Difficulty: %s
            
            Evaluate based on:
            1. Correctness - Did they answer correctly?
            2. Clarity - Was the answer clear and well-structured?
            3. Communication - How well did they communicate?
            4. Depth - Did they show deep understanding?
            
            Return ONLY this JSON format:
            {
                "score": 85,
                "isCorrect": true,
                "feedback": "Detailed feedback on the answer",
                "strengths": ["Strength 1", "Strength 2"],
                "weaknesses": ["Weakness 1", "Weakness 2"],
                "suggestions": "Improvement suggestions",
                "communicationScore": 80,
                "clarityScore": 85,
                "correctnessScore": 90,
                "followUpNeeded": true,
                "followUpQuestion": "Optional follow-up question if needed"
            }
            """,
                question.getQuestionText(),
                question.getQuestionType(),
                answer.getAnswerText(),
                session.getRole(),
                session.getDifficulty());

        return callOpenRouter(systemPrompt, userPrompt);
    }

    @Override
    public AIResponse evaluateCodingSolution(String problem, String solution, String language, String role, String difficulty) {
        log.debug("Evaluating coding solution with OpenRouter");

        String systemPrompt = """
            You are WAVE, an expert coding interview evaluator.
            Evaluate the code for correctness, efficiency, and quality.
            Return ONLY valid JSON, no other text.
            """;

        String userPrompt = String.format("""
            Evaluate the following coding solution.
            
            Problem: %s
            Language: %s
            Candidate Solution: %s
            Role: %s
            Difficulty: %s
            
            Evaluate based on:
            1. Correctness (Does it solve the problem?)
            2. Efficiency (Time and space complexity)
            3. Code Quality (Readability, naming, structure)
            4. Edge Cases (Does it handle edge cases?)
            
            Return ONLY this JSON format:
            {
                "score": 85,
                "isCorrect": true,
                "feedback": "Detailed feedback",
                "strengths": ["Strength 1", "Strength 2"],
                "weaknesses": ["Weakness 1", "Weakness 2"],
                "timeComplexity": "O(n)",
                "spaceComplexity": "O(n)",
                "suggestions": "Add input validation for edge cases and more comments for clarity.",
                "optimizedSolution": "Optimized code if applicable"
            }
            """, problem, language, solution, role, difficulty);

        return callOpenRouter(systemPrompt, userPrompt);
    }

    @Override
    public AIResponse chat(String userMessage, List<AIRequest> conversationHistory) {
        log.debug("Processing chat message with OpenRouter");

        String systemPrompt = """
            You are WAVE, a helpful AI interview assistant.
            Respond to the user's messages professionally and helpfully.
            """;

        String userPrompt = String.format("User message: %s\n\nProvide a helpful response.", userMessage);

        return callOpenRouter(systemPrompt, userPrompt);
    }

    @Override
    public AIResponse getRecommendations(String role, String difficulty, String performanceData) {
        log.debug("Generating recommendations with OpenRouter");

        String systemPrompt = """
            You are WAVE, a career development expert.
            Provide personalized recommendations based on performance.
            Return ONLY valid JSON, no other text.
            """;

        String userPrompt = String.format("""
            Based on the following performance data, provide recommendations.
            
            Role: %s
            Difficulty: %s
            Performance: %s
            
            Return ONLY this JSON format:
            {
                "overallAssessment": "Summary of performance",
                "strengths": ["Strength 1", "Strength 2"],
                "areasForImprovement": ["Area 1", "Area 2"],
                "recommendations": ["Recommendation 1", "Recommendation 2"],
                "nextSteps": "What to do next",
                "estimatedPreparationTime": "Time needed for improvement"
            }
            """, role, difficulty, performanceData);

        return callOpenRouter(systemPrompt, userPrompt);
    }

    @Override
    public AIResponse evaluateSession(InterviewSession session, List<Question> questions, List<Answer> answers) {
        log.debug("Evaluating interview session with OpenRouter: {}", session.getSessionId());

        String systemPrompt = """
            You are WAVE, an expert AI interviewer.
            Your task is to conduct a final comprehensive evaluation of the candidate's performance during the interview.
            Analyze their answers, technical accuracy, clarity, communication, and confidence.
            Return ONLY a valid JSON object. Do not include markdown code blocks, comments or any other text outside the JSON.
            """;

        // Build QA text
        StringBuilder qaBuilder = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            final long qId = q.getId();
            Optional<Answer> aOpt = answers.stream().filter(ans -> ans.getQuestion().getId().equals(qId)).findFirst();
            qaBuilder.append("Question ").append(q.getSequenceNumber()).append(": ").append(q.getQuestionText()).append("\n");
            if (aOpt.isPresent()) {
                Answer a = aOpt.get();
                qaBuilder.append("Candidate Answer: ").append(a.getAnswerText()).append("\n");
                qaBuilder.append("Answer Evaluation Score: ").append(a.getScore() != null ? Math.round(a.getScore() * 100) : "N/A").append("%\n");
                qaBuilder.append("Feedback: ").append(a.getAiFeedback()).append("\n");
            } else {
                qaBuilder.append("Candidate Answer: [No Answer Provided]\n");
            }
            qaBuilder.append("\n");
        }

        String userPrompt = String.format("""
            Please perform a comprehensive review of this interview session and generate a detailed report.
            
            Role: %s
            Difficulty: %s
            Mode: %s
            
            Interview Transcript:
            %s
            
            Based on the candidate's answers and individual evaluations, calculate their overall scores out of 100:
            1. overallScore (overall performance score)
            2. communicationScore (fluency, clarity of speech/writing, professional tone)
            3. clarityScore (how structured and understandable their answers were)
            4. correctnessScore (how technically correct their answers were)
            
            Generate a detailed feedback report in feedbackSummary. The report should be a professional, detailed assessment of:
            - Technical Knowledge & Competence
            - Communication Skills
            - Completeness and Correctness of answers
            This report MUST be formatted in clean, professional markdown with clear headings, lists, and spacing (e.g. ### Technical Knowledge, ### Communication, etc.). Use newlines and lists to make it highly readable.
            
            Also extract a list of 2-3 specific strengths, 2-3 weaknesses, and 3-4 clear actionable improvement suggestions.
            
            Return ONLY this JSON structure:
            {
                "score": 78.5,
                "communicationScore": 80.0,
                "clarityScore": 75.0,
                "correctnessScore": 82.0,
                "feedbackSummary": "### Overall Assessment\\n\\nThe candidate demonstrated...\\n\\n### Technical Competence\\n\\n- Strong understanding of...\\n- Gaps in...",
                "strengths": ["Strong understanding of core concepts", "Clear structural explanations"],
                "weaknesses": ["Could provide more concrete design patterns", "Struggled with edge case analysis"],
                "suggestions": ["Practice explaining distributed systems architectures", "Study Singleton thread-safety implementations"]
            }
            """, session.getRole(), session.getDifficulty(), session.getMode(), qaBuilder.toString());

        return callOpenRouter(systemPrompt, userPrompt);
    }

    @Override
    public boolean isAvailable() {
        return aiConfig.getApiKey() != null && !aiConfig.getApiKey().isEmpty();
    }

    @Override
    public String getProviderName() {
        return "openrouter";
    }

    /**
     * Core method to call OpenRouter API using RestTemplate
     */
    private AIResponse callOpenRouter(String systemPrompt, String userPrompt) {
        long startTime = System.currentTimeMillis();

        try {
            // Build request body using Jackson
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", aiConfig.getModel());
            requestBody.put("temperature", aiConfig.getTemperature());
            requestBody.put("max_tokens", aiConfig.getMaxTokens());

            // Build messages array
            ArrayNode messagesArray = objectMapper.createArrayNode();

            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messagesArray.add(systemMessage);

            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messagesArray.add(userMessage);

            requestBody.set("messages", messagesArray);

            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + aiConfig.getApiKey());
            headers.set("HTTP-Referer", siteUrl);
            headers.set("X-Title", siteName);

            HttpEntity<ObjectNode> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling OpenRouter API with model: {}", aiConfig.getModel());

            // Make the API call
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    "https://openrouter.ai/api/v1/chat/completions",
                    entity,
                    String.class
            );

            String response = responseEntity.getBody();

            if (response == null) {
                log.error("OpenRouter returned null response");
                return createErrorResponse("No response from OpenRouter", System.currentTimeMillis() - startTime);
            }

            // Parse response using Jackson
            JsonNode jsonResponse = objectMapper.readTree(response);
            String content = jsonResponse
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            // Clean the content (remove markdown code blocks if present)
            content = cleanJsonContent(content);

            log.debug("OpenRouter response received in {}ms", System.currentTimeMillis() - startTime);

            // Validate JSON
            try {
                objectMapper.readTree(content);
            } catch (Exception e) {
                log.warn("Response is not valid JSON, but continuing: {}",
                        content.substring(0, Math.min(100, content.length())));
            }

            return AIResponse.builder()
                    .content(content)
                    .rawResponse(response)
                    .success(true)
                    .metadata(Map.of(
                            "type", "openrouter",
                            "model", aiConfig.getModel(),
                            "timestamp", LocalDateTime.now().toString()
                    ))
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Error calling OpenRouter API: {}", e.getMessage(), e);
            return createErrorResponse(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Clean JSON content from markdown code blocks
     */
    private String cleanJsonContent(String content) {
        if (content == null) return null;

        // Remove ```json and ``` markers
        content = content.replaceAll("```json\\s*", "");
        content = content.replaceAll("```\\s*", "");
        content = content.trim();

        // If it starts with { or [, it's likely JSON
        if (content.startsWith("{") || content.startsWith("[")) {
            return content;
        }

        // Try to find JSON object in the content
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }

        return content;
    }

    /**
     * Create error response
     */
    private AIResponse createErrorResponse(String errorMessage, long processingTime) {
        return AIResponse.builder()
                .content("")
                .success(false)
                .errorMessage(errorMessage)
                .processingTimeMs(processingTime)
                .build();
    }
}
