package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionResponse {
    private Long id;
    private String sessionId;
    private String mode;
    private String role;
    private String difficulty;
    private Integer timeLimit;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Integer correctAnswers;
    private Double score;
    private Double communicationScore;
    private Double clarityScore;
    private Double correctnessScore;
    private String status;
    private String feedbackSummary;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private List<QuestionResponse> questions;
}