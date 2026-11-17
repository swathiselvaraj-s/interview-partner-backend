package com.interviewpartner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance_trends")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate trendDate;

    @Column
    private Double overallScore;

    @Column
    private Double communicationScore;

    @Column
    private Double clarityScore;

    @Column
    private Double correctnessScore;

    @Column
    private Integer totalInterviews;

    @Column
    private Double improvementRate; // Percentage improvement from previous

    @Column(columnDefinition = "JSON")
    private String categoryScores; // JSON map of category scores

    @CreationTimestamp
    private LocalDateTime createdAt;
}