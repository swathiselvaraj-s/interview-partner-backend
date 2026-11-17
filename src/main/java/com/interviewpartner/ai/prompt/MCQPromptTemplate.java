package com.interviewpartner.ai.prompt;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MCQPromptTemplate {

    private static final String SYSTEM_PROMPT = """
        You are WAVE, an expert AI interview assistant specializing in generating high-quality multiple-choice questions.
        Generate clear, accurate, and challenging MCQs with exactly 4 options.
        Only one option should be correct.
        Provide the correct answer index (0-3).
        """;

    private static final String USER_PROMPT_TEMPLATE = """
        Generate a multiple-choice question for a %s interview at %s level.
        
        Topic: %s
        Difficulty: %s (1-5, where 5 is most difficult)
        
        Requirements:
        - Question should test %s knowledge
        - Provide exactly 4 options (A, B, C, D)
        - Clearly mark the correct answer
        - Include a brief explanation of why the answer is correct
        
        Format the response as JSON:
        {
            "question": "The question text",
            "options": ["Option A", "Option B", "Option C", "Option D"],
            "correctIndex": 0,
            "explanation": "Why this is the correct answer",
            "topic": "Specific topic covered",
            "difficulty": 3
        }
        """;

    public static String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public static String getUserPrompt(String role, String difficulty, String topic) {
        return String.format(USER_PROMPT_TEMPLATE, role, difficulty, topic, difficulty, role);
    }
}