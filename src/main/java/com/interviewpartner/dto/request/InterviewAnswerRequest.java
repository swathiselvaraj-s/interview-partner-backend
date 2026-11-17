package com.interviewpartner.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAnswerRequest {

    private String answerText;
    private Integer selectedOptionIndex; // For MCQ
    private Integer timeSpentSeconds;

    // For coding mode
    private String codeContent;
    private String codeLanguage;
}