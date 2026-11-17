package com.interviewpartner.interview.enums;

public enum QuestionType {
    TEXT("Text-based"),
    MCQ("Multiple Choice"),
    CODING("Coding Problem");

    private final String label;

    QuestionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}