package com.interviewpartner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String mode;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String difficulty;

    @Column
    private Integer timeLimit;

    @Column
    private Integer totalQuestions;

    @Column
    private Integer answeredQuestions;

    @Column
    private Integer correctAnswers;

    @Column
    private Double score;

    @Column
    private Double communicationScore;

    @Column
    private Double clarityScore;

    @Column
    private Double correctnessScore;

    @Column
    private String status;

    @Column(columnDefinition = "TEXT")
    private String feedbackSummary;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    // ✅ Only ONE definition of selectedTopics
    @Column(columnDefinition = "JSON")
    private String selectedTopics;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}