package com.interviewpartner.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferencesRequest {
    private String preferredRole;
    private String preferredDifficulty;
    private String preferredMode;
    private Integer experienceYears;
    private String availability;
    private String timezone;
}