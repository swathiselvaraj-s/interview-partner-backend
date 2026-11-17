package com.interviewpartner.service;

import com.interviewpartner.dto.request.ChangePasswordRequest;
import com.interviewpartner.dto.request.UpdatePreferencesRequest;
import com.interviewpartner.dto.request.UpdateProfileRequest;
import com.interviewpartner.dto.request.UserSearchRequest;
import com.interviewpartner.dto.response.UserProfileResponse;
import com.interviewpartner.dto.response.UserResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {
    UserProfileResponse getCurrentUserProfile(String email);
    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);
    UserProfileResponse updatePreferences(String email, UpdatePreferencesRequest request);
    void changePassword(String email, ChangePasswordRequest request);
    void deleteAccount(String email);
    List<UserResponse> getAllUsers(int page, int size);
    UserResponse getUserById(Long id);
    void updateLastLogin(String email);
    boolean isEmailAvailable(String email);
    UserProfileResponse getPublicProfile(Long userId);
    Page<UserResponse> searchUsers(UserSearchRequest request);
}