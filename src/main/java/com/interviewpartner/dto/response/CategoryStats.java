package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStats {
    private String category;
    private Double score;
    private Integer questionsAttempted;
    private Integer questionsCorrect;
    private Double accuracy;
    private Double averageTime;
    private String trend; // IMPROVING, STABLE, DECLINING
}
