package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    private OverallStats overallStats;
    private List<CategoryStats> categoryStats;
    private Map<String, Double> categoryScores;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
    private String strongestCategory;
    private String weakestCategory;
    private Double consistencyScore;
    private ProgressData progressData;
}

