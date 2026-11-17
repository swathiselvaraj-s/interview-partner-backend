package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendData {
    private String date;
    private Double score;
    private Double communicationScore;
    private Double clarityScore;
    private Double correctnessScore;
    private Integer interviews;
}
