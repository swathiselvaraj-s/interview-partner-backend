package com.interviewpartner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "answers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answerText;

    @Column
    private Integer selectedOptionIndex;

    @Column
    private Boolean isCorrect;

    @Column
    private Double score;  // Changed from BigDecimal

    @Column(columnDefinition = "TEXT")
    private String aiFeedback;

    @Column
    private Integer timeSpentSeconds;

    @Column
    private String codeLanguage;

    @Column(columnDefinition = "TEXT")
    private String codeOutput;

    @Column(columnDefinition = "TEXT")
    private String executionLog;

    @Column
    private Double communicationScore;  // Changed from BigDecimal

    @Column
    private Double clarityScore;  // Changed from BigDecimal

    @Column
    private Double correctnessScore;  // Changed from BigDecimal

    @Column
    private LocalDateTime answeredAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}