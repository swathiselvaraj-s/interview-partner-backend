package com.interviewpartner.service.analytics;

import com.interviewpartner.dto.response.AnalyticsResponse;
import com.interviewpartner.entity.InterviewSession;

import java.time.LocalDate;
import java.util.Map;

public interface AnalyticsService {

    // Update analytics after each answer
    void updateSessionAnalytics(InterviewSession session);

    // Finalize analytics when session ends
    void finalizeSessionAnalytics(InterviewSession session);

    // Get complete analytics for a user
    AnalyticsResponse getUserAnalytics(String email);

    // Get category-specific analytics
    Map<String, Double> getCategoryScores(String email);

    // Get trend data for charts
    AnalyticsResponse getTrendData(String email, int days);

    // Get weak topics
    Map<String, Double> getWeakTopics(String email);

    // Get recommendations for improvement
    Map<String, Object> getRecommendations(String email);

    // Track daily performance
    void trackDailyPerformance(String email);

    // Calculate improvement metrics
    Map<String, Object> calculateImprovement(String email);
}