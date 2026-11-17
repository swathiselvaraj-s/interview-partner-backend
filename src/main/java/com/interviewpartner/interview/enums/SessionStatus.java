package com.interviewpartner.interview.enums;

public enum SessionStatus {
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    ABANDONED("Abandoned"),
    TIMED_OUT("Timed Out");

    private final String label;

    SessionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}