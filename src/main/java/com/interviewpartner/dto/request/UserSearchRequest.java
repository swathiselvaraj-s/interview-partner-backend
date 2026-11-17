package com.interviewpartner.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {
    private String keyword;
    private String preferredRole;
    private String preferredDifficulty;
    private Integer minExperience;
    private Integer maxExperience;
    private Boolean active;
    private String sortBy;
    private String sortDirection;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}