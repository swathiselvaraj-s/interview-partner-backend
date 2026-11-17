package com.interviewpartner.ai.prompt;

import lombok.experimental.UtilityClass;

@UtilityClass
public class InterviewPromptTemplate {

    private static final String SYSTEM_PROMPT = """
        You are WAVE, an expert AI interviewer conducting a professional job interview.
        Your role is to assess the candidate's knowledge, communication skills, and problem-solving abilities.
        Ask relevant, challenging questions based on the role and difficulty level.
        Evaluate answers objectively and provide constructive feedback.
        """;

    private static final String USER_PROMPT_TEMPLATE = """
        Conduct an interview with a candidate for a %s position at %s level.
        
        Current question number: %d
        Candidate's previous answer: %s
        
        Based on the conversation context, generate the next question.
        Consider the candidate's previous answer quality and adjust accordingly.
        
        Requirements:
        1. Ask one clear, focused question
        2. Question should be relevant to %s role
        3. Difficulty should match %s level
        4. Include a brief context or scenario if appropriate
        
        Format the response as JSON:
        {
            "question": "The interview question",
            "context": "Additional context or scenario",
            "topic": "Topic being assessed",
            "difficulty": 2,
            "followUp": true,
            "expectedSkills": ["Skill 1", "Skill 2"]
        }
        """;

    public static String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public static String getUserPrompt(String role, String difficulty, int questionNumber,
                                       String previousAnswer) {
        return String.format(USER_PROMPT_TEMPLATE,
                role, difficulty, questionNumber, previousAnswer,
                role, difficulty);
    }
}