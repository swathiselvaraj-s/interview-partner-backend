package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {
    private Long id;
    private String answerText;
    private Integer selectedOptionIndex;
    private Boolean isCorrect;
    private Double score;
    private String aiFeedback;
    private Integer timeSpentSeconds;
    private Double communicationScore;
    private Double clarityScore;
    private Double correctnessScore;
    private LocalDateTime answeredAt;
}