package com.interviewpartner.ai.prompt;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FollowUpPromptTemplate {

    private static final String SYSTEM_PROMPT = """
        You are WAVE, an expert AI interviewer specializing in asking insightful follow-up questions.
        Based on the candidate's answer, ask a follow-up question that probes deeper into the topic.
        Focus on areas where the candidate showed gaps in understanding or could demonstrate deeper knowledge.
        """;

    private static final String USER_PROMPT_TEMPLATE = """
        Based on the candidate's answer below, generate a relevant follow-up question.
        
        Role: %s
        Difficulty: %s
        Original Question: %s
        Candidate's Answer: %s
        
        The follow-up should:
        1. Probe deeper into the topic
        2. Address any gaps in the candidate's answer
        3. Encourage critical thinking
        4. Assess deeper understanding
        
        Format the response as JSON:
        {
            "question": "The follow-up question",
            "context": "Why this follow-up is relevant",
            "difficulty": 3,
            "topic": "Topic being probed",
            "skillsAssessed": ["Skill 1", "Skill 2"]
        }
        """;

    public static String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public static String getUserPrompt(String role, String difficulty, String originalQuestion,
                                       String candidateAnswer) {
        return String.format(USER_PROMPT_TEMPLATE, role, difficulty, originalQuestion, candidateAnswer);
    }
}