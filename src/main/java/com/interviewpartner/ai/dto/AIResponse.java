package com.interviewpartner.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
    private String content;
    private String rawResponse;
    private Map<String, Object> metadata;
    private boolean success;
    private String errorMessage;
    private Long processingTimeMs;
}