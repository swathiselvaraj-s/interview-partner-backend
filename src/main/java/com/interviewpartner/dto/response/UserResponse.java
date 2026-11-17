package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String preferredRole;
    private String preferredDifficulty;
    private String preferredMode;
    private Integer experienceYears;
    private String bio;
    private String role;
    private LocalDateTime createdAt;
    private boolean active;
}