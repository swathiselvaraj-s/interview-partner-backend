package com.interviewpartner.ai.prompt;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CodingPromptTemplate {

    private static final String SYSTEM_PROMPT = """
        You are WAVE, an expert coding interview assistant specializing in evaluating code quality and logic.
        Generate coding problems and evaluate solutions based on correctness, efficiency, and code quality.
        Provide detailed feedback on the solution's approach, time complexity, and space complexity.
        """;

    private static final String GENERATE_PROMPT_TEMPLATE = """
        Generate a coding problem for a %s interview at %s level.
        
        Topic: %s
        Difficulty: %d/5
        
        Requirements:
        - Problem should test %s concepts
        - Include input/output examples
        - Specify constraints
        - Include concept tags
        - Include public and hidden tests for automated judging
        - Indicate expected time and space complexity
        
        Format the response as JSON:
        {
            "title": "Problem title",
            "description": "Problem description",
            "difficulty": 3,
            "tags": ["array", "hash map"],
            "examples": [
                {
                    "input": "example input",
                    "output": "expected output",
                    "explanation": "why this is correct"
                }
            ],
            "constraints": ["Constraint 1", "Constraint 2"],
            "starterCode": "Code template in the target language",
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
        """;

    private static final String EVALUATE_PROMPT_TEMPLATE = """
        Evaluate the following coding solution for a %s problem at %s level.
        
        Problem: %s
        Candidate's Solution: %s
        Expected Language: %s
        
        Evaluate based on:
        1. Correctness (Does it solve the problem?)
        2. Efficiency (Time and space complexity)
        3. Code Quality (Readability, naming, structure)
        4. Edge Cases (Does it handle edge cases?)
        
        Provide detailed feedback including:
        - Overall score (0-100)
        - Specific issues found
        - Suggestions for improvement
        - Corrected or optimized solution
        
        Format the response as JSON:
        {
            "score": 85,
            "isCorrect": true,
            "feedback": "Detailed feedback on the solution",
            "strengths": ["Strength 1", "Strength 2"],
            "weaknesses": ["Weakness 1", "Weakness 2"],
            "timeComplexity": "O(n)",
            "spaceComplexity": "O(1)",
            "suggestions": "Improvement suggestions",
            "optimizedSolution": "Optimized code solution if applicable"
        }
        """;

    public static String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public static String getGeneratePrompt(String role, String difficulty, String topic, int level) {
        return String.format(GENERATE_PROMPT_TEMPLATE, role, difficulty, topic, level, role);
    }

    public static String getEvaluatePrompt(String role, String difficulty, String problem,
                                           String solution, String language) {
        return String.format(EVALUATE_PROMPT_TEMPLATE, role, difficulty, problem, solution, language);
    }
}
