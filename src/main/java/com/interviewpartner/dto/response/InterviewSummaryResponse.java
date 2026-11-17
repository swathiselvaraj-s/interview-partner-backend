package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSummaryResponse {
    private String sessionId;
    private String mode;
    private String role;
    private String difficulty;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Integer correctAnswers;
    private Double overallScore;
    private Map<String, Double> categoryScores; // Category-wise scores
    private String feedbackSummary;
    private List<String> strengths;
    private List<String> weaknesses;
    private String improvementSuggestions;
    private List<QuestionWithAnswerResponse> questionDetails;

    // Optional telemetry for voice mode & eye contact monitoring
    private Double eyeContactScore;
    private Integer eyeContactWarnings;
    private Integer timeLookingAwaySeconds;
    private Double speakingPaceWpm;
    private Double totalSpeechDurationSeconds;
    private Double averageResponseLatencySeconds;
    private Integer fillerWordCount;
}