package com.interviewpartner.controller;

import com.interviewpartner.dto.response.AnalyticsResponse;
import com.interviewpartner.service.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsResponse> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting dashboard analytics for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(analyticsService.getUserAnalytics(userDetails.getUsername()));
    }

    @GetMapping("/scores")
    public ResponseEntity<Map<String, Double>> getCategoryScores(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting category scores for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(analyticsService.getCategoryScores(userDetails.getUsername()));
    }

    @GetMapping("/trends")
    public ResponseEntity<AnalyticsResponse> getTrends(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "30") int days) {
        log.debug("Getting trend data for user: {}, days: {}", userDetails.getUsername(), days);
        return ResponseEntity.ok(analyticsService.getTrendData(userDetails.getUsername(), days));
    }

    @GetMapping("/weak-topics")
    public ResponseEntity<Map<String, Double>> getWeakTopics(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting weak topics for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(analyticsService.getWeakTopics(userDetails.getUsername()));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendations(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting recommendations for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(analyticsService.getRecommendations(userDetails.getUsername()));
    }

    @GetMapping("/improvement")
    public ResponseEntity<Map<String, Object>> getImprovement(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting improvement metrics for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(analyticsService.calculateImprovement(userDetails.getUsername()));
    }
}