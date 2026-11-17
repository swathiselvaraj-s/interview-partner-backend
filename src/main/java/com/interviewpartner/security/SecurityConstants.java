package com.interviewpartner.security;

public class SecurityConstants {

    public static final String[] PUBLIC_URLS = {
            "/auth/**",
            "/public/**",
            "/health",
            "/",
            "/actuator/health"
    };

    public static final String[] SWAGGER_URLS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**"
    };
}