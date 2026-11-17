package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String questionText;
    private String questionType;
    private String category;
    private Integer difficulty;
    private Integer sequenceNumber;
    private List<String> options; // For MCQ
    private String title;
    private String description;
    private List<Map<String, String>> examples;
    private List<String> constraints;
    private List<String> tags;
    private String starterCode;
    private String language;
    private Map<String, String> expectedComplexity;
    private List<Map<String, String>> publicTests;
    private List<Map<String, String>> hiddenTests;
    private List<AnswerResponse> answers;
    private LocalDateTime createdAt;
}
