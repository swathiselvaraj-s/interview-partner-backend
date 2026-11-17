package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverallStats {
    private Double averageScore;
    private Double maxScore;
    private Double minScore;
    private Double communicationScore;
    private Double clarityScore;
    private Double correctnessScore;
    private Integer totalInterviews;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Double accuracyRate;
    private Double averageTimePerQuestion;
    private Double averageInterviewDuration;
    private Integer dayStreak;
}
