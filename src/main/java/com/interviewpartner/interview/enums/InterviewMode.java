package com.interviewpartner.interview.enums;

public enum InterviewMode {
    CHAT("Chat-based AI Interview"),
    MCQ("Multiple Choice Questions"),
    CODING("Coding Interview"),
    VOICE("Voice-based Interview"),
    CUSTOM("Custom Mode");

    private final String description;

    InterviewMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}