package com.interviewpartner.service.impl;

import com.interviewpartner.dto.request.LoginRequest;
import com.interviewpartner.dto.request.RegisterRequest;
import com.interviewpartner.dto.response.LoginResponse;
import com.interviewpartner.dto.response.UserResponse;
import com.interviewpartner.entity.User;
import com.interviewpartner.exception.BadRequestException;
import com.interviewpartner.repository.UserRepository;
import com.interviewpartner.security.JwtTokenProvider;
import com.interviewpartner.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.debug("Register attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role("ROLE_USER")
                .preferredRole(request.getPreferredRole())
                .preferredDifficulty(request.getPreferredDifficulty())
                .preferredMode(request.getPreferredMode())
                .experienceYears(request.getExperienceYears())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        return mapToUserResponse(savedUser);
    }

    @Override
    public void logout(String token) {
        // JWT is stateless, so we don't need to invalidate it
        // But we can add token to blacklist if needed
        SecurityContextHolder.clearContext();
        log.debug("User logged out");
    }

    @Override
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .preferredRole(user.getPreferredRole())
                .preferredDifficulty(user.getPreferredDifficulty())
                .preferredMode(user.getPreferredMode())
                .experienceYears(user.getExperienceYears())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .active(user.isActive())
                .build();
    }
}