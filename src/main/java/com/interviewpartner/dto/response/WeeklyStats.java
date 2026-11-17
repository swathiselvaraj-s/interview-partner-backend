package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStats {
    private String week;
    private Double averageScore;
    private Integer totalInterviews;
    private Double improvement;
}
