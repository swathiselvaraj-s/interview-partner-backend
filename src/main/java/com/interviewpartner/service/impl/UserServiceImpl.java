package com.interviewpartner.service.impl;

import com.interviewpartner.dto.request.ChangePasswordRequest;
import com.interviewpartner.dto.request.UpdatePreferencesRequest;
import com.interviewpartner.dto.request.UpdateProfileRequest;
import com.interviewpartner.dto.request.UserSearchRequest;
import com.interviewpartner.dto.response.UserProfileResponse;
import com.interviewpartner.dto.response.UserResponse;
import com.interviewpartner.entity.User;
import com.interviewpartner.exception.BadRequestException;
import com.interviewpartner.exception.ResourceNotFoundException;
import com.interviewpartner.repository.UserRepository;
import com.interviewpartner.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponse getCurrentUserProfile(String email) {
        User user = getUserByEmail(email);
        return mapToUserProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getUserByEmail(email);

        // Check if email is being changed and if it's already taken
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already taken");
            }
            user.setEmail(request.getEmail());
        }

        // Update fields if present
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPreferredRole() != null) user.setPreferredRole(request.getPreferredRole());
        if (request.getPreferredDifficulty() != null) user.setPreferredDifficulty(request.getPreferredDifficulty());
        if (request.getPreferredMode() != null) user.setPreferredMode(request.getPreferredMode());
        if (request.getExperienceYears() != null) user.setExperienceYears(request.getExperienceYears());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getLinkedinUrl() != null) user.setLinkedinUrl(request.getLinkedinUrl());
        if (request.getGithubUrl() != null) user.setGithubUrl(request.getGithubUrl());
        if (request.getPortfolioUrl() != null) user.setPortfolioUrl(request.getPortfolioUrl());
        if (request.getCurrentCompany() != null) user.setCurrentCompany(request.getCurrentCompany());
        if (request.getCurrentPosition() != null) user.setCurrentPosition(request.getCurrentPosition());
        if (request.getEducation() != null) user.setEducation(request.getEducation());
        if (request.getSkills() != null) user.setSkills(request.getSkills());
        if (request.getAvailability() != null) user.setAvailability(request.getAvailability());
        if (request.getTimezone() != null) user.setTimezone(request.getTimezone());
        if (request.getNotificationsEnabled() != null) user.setNotificationsEnabled(request.getNotificationsEnabled());
        if (request.getEmailUpdatesEnabled() != null) user.setEmailUpdatesEnabled(request.getEmailUpdatesEnabled());

        User updatedUser = userRepository.save(user);
        log.info("User profile updated for: {}", email);

        return mapToUserProfileResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserProfileResponse updatePreferences(String email, UpdatePreferencesRequest request) {
        User user = getUserByEmail(email);

        if (request.getPreferredRole() != null) {
            user.setPreferredRole(request.getPreferredRole());
        }
        if (request.getPreferredDifficulty() != null) {
            user.setPreferredDifficulty(request.getPreferredDifficulty());
        }
        if (request.getPreferredMode() != null) {
            user.setPreferredMode(request.getPreferredMode());
        }
        if (request.getExperienceYears() != null) {
            user.setExperienceYears(request.getExperienceYears());
        }
        if (request.getAvailability() != null) {
            user.setAvailability(request.getAvailability());
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }

        User updatedUser = userRepository.save(user);
        log.info("User preferences updated for: {}", email);

        return mapToUserProfileResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserByEmail(email);

        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Validate new password and confirm password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", email);
    }

    @Override
    @Transactional
    public void deleteAccount(String email) {
        User user = getUserByEmail(email);
        user.setActive(false);
        userRepository.save(user);
        log.info("Account deactivated for user: {}", email);
    }

    @Override
    public List<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void updateLastLogin(String email) {
        User user = getUserByEmail(email);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Override
    public UserProfileResponse getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Don't expose sensitive information in public profile
        UserProfileResponse response = mapToUserProfileResponse(user);
        response.setEmail(null); // Hide email in public profile
        return response;
    }

    // Helper Methods
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        List<String> skillsList = user.getSkills() != null && !user.getSkills().isEmpty()
                ? Arrays.stream(user.getSkills().split(","))
                  .map(String::trim)
                  .collect(Collectors.toList())
                : List.of();

        // Calculate stats (will be enhanced when interview module is ready)
        Integer totalInterviews = user.getInterviewSessions() != null ? user.getInterviewSessions().size() : 0;

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole())
                .preferredRole(user.getPreferredRole())
                .preferredDifficulty(user.getPreferredDifficulty())
                .preferredMode(user.getPreferredMode())
                .experienceYears(user.getExperienceYears())
                .bio(user.getBio())
                .linkedinUrl(user.getLinkedinUrl())
                .githubUrl(user.getGithubUrl())
                .portfolioUrl(user.getPortfolioUrl())
                .currentCompany(user.getCurrentCompany())
                .currentPosition(user.getCurrentPosition())
                .education(user.getEducation())
                .skills(skillsList)
                .availability(user.getAvailability())
                .timezone(user.getTimezone())
                .totalInterviews(totalInterviews)
                .averageScore(0.0) // Will be calculated from actual data
                .strongestSkill("Not analyzed yet")
                .weakestSkill("Not analyzed yet")
                .profilePicture(user.getProfilePicture())
                .notificationsEnabled(user.isNotificationsEnabled())
                .emailUpdatesEnabled(user.isEmailUpdatesEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
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
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .active(user.isActive())
                .build();
    }
    @Override
    public Page<UserResponse> searchUsers(UserSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        return userRepository.searchUsers(
                request.getKeyword(),
                request.getPreferredRole(),
                request.getPreferredDifficulty(),
                request.getMinExperience(),
                request.getMaxExperience(),
                request.getActive(),
                pageable
        ).map(this::mapToUserResponse);
    }
}