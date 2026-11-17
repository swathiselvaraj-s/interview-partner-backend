package com.interviewpartner.service;

import com.interviewpartner.dto.request.LoginRequest;
import com.interviewpartner.dto.request.RegisterRequest;
import com.interviewpartner.dto.response.LoginResponse;
import com.interviewpartner.dto.response.UserResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    UserResponse register(RegisterRequest request);
    void logout(String token);
    boolean validateToken(String token);
}