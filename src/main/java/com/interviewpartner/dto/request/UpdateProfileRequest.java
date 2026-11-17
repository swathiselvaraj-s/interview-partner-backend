package com.interviewpartner.dto.request;

import com.interviewpartner.annotation.ValidUrl;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    private String preferredRole;
    private String preferredDifficulty;
    private String preferredMode;
    private Integer experienceYears;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    @ValidUrl
    private String linkedinUrl;

    @ValidUrl
    private String githubUrl;

    @ValidUrl
    private String portfolioUrl;

    private String currentCompany;
    private String currentPosition;
    private String education;
    private String skills;
    private String availability;
    private String timezone;
    private Boolean notificationsEnabled;
    private Boolean emailUpdatesEnabled;
}