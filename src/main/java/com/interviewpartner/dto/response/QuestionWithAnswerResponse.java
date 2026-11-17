package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionWithAnswerResponse {
    private QuestionResponse question;
    private AnswerResponse answer;
    private boolean hasAnswered;
}