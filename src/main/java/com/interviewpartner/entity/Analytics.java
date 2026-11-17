package com.interviewpartner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private InterviewSession session;

    @Column(nullable = false)
    private String category; // OVERALL, JAVA, DSA, FRONTEND, BACKEND, etc.

    @Column
    private Double score;

    @Column
    private Double communicationScore;

    @Column
    private Double clarityScore;

    @Column
    private Double correctnessScore;

    @Column
    private Integer questionsAttempted;

    @Column
    private Integer questionsCorrect;

    @Column
    private Double averageTimePerQuestion; // in seconds

    @Column(columnDefinition = "JSON")
    private String strengths; // JSON array

    @Column(columnDefinition = "JSON")
    private String weaknesses; // JSON array

    @Column(columnDefinition = "JSON")
    private String recommendations; // JSON array

    @Column
    private String difficultyLevel;

    @Column
    private String interviewMode;

    @Column
    private LocalDateTime recordedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}