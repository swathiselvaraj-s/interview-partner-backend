package com.interviewpartner.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewEndRequest {
    private boolean abandon;

    // Optional telemetry for voice mode & eye contact monitoring
    private Double eyeContactScore;
    private Integer eyeContactWarnings;
    private Integer timeLookingAwaySeconds;
    private Double speakingPaceWpm;
    private Double totalSpeechDurationSeconds;
    private Double averageResponseLatencySeconds;
    private Integer fillerWordCount;
}