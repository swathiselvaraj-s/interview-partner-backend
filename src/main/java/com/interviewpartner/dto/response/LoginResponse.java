package com.interviewpartner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;

    @Builder.Default  // ✅ Add this to fix the warning
    private String type = "Bearer";

    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}