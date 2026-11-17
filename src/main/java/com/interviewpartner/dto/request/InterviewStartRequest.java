package com.interviewpartner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewStartRequest {

    @NotBlank(message = "Mode is required")
    private String mode; // CHAT, MCQ, CODING, VOICE, CUSTOM

    @NotBlank(message = "Role is required")
    private String role; // JAVA, FRONTEND, BACKEND, DSA, etc.

    @NotBlank(message = "Difficulty is required")
    private String difficulty; // BEGINNER, INTERMEDIATE, ADVANCED

    private Integer timeLimit; // in minutes, optional

    private Integer numberOfQuestions; // optional, defaults based on mode

    private String customPrompt; // for custom mode

    private List<String> topics;
}