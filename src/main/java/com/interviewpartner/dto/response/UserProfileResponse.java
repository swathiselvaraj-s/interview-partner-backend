package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String role;

    // Preferences
    private String preferredRole;
    private String preferredDifficulty;
    private String preferredMode;
    private Integer experienceYears;

    // Professional Info
    private String bio;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String currentCompany;
    private String currentPosition;
    private String education;
    private List<String> skills;
    private String availability;
    private String timezone;

    // Stats
    private Integer totalInterviews;
    private Double averageScore;
    private String strongestSkill;
    private String weakestSkill;

    // Metadata
    private String profilePicture;
    private boolean notificationsEnabled;
    private boolean emailUpdatesEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}