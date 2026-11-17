package com.interviewpartner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String role; // ROLE_USER, ROLE_ADMIN

    // Preferences
    @Column
    private String preferredRole; // JAVA, FRONTEND, BACKEND, DSA, etc.

    @Column
    private String preferredDifficulty; // BEGINNER, INTERMEDIATE, ADVANCED

    @Column
    private String preferredMode; // CHAT, MCQ, CODING, VOICE

    @Column
    private Integer experienceYears;

    @Column
    private String bio;

    @Column
    private String linkedinUrl;

    @Column
    private String githubUrl;

    @Column
    private String portfolioUrl;

    @Column
    private String currentCompany;

    @Column
    private String currentPosition;

    @Column
    private String education;

    @Column
    private String skills; // Comma separated skills

    @Column
    private String availability; // FULL_TIME, PART_TIME, CASUAL

    @Column
    private String timezone;

    @Column
    private String profilePicture;

    @Column
    @Builder.Default
    private boolean notificationsEnabled = true;

    @Column
    @Builder.Default
    private boolean emailUpdatesEnabled = true;


    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Resume resume;


    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<InterviewSession> interviewSessions = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @Column
    private boolean active = true;


}