package com.interviewpartner.controller;

import com.interviewpartner.dto.request.InterviewAnswerRequest;
import com.interviewpartner.dto.request.InterviewEndRequest;
import com.interviewpartner.dto.request.InterviewStartRequest;
import com.interviewpartner.dto.response.InterviewSessionResponse;
import com.interviewpartner.dto.response.InterviewSummaryResponse;
import com.interviewpartner.dto.response.QuestionResponse;
import com.interviewpartner.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    public ResponseEntity<InterviewSessionResponse> startInterview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InterviewStartRequest request) {
        log.debug("Starting interview for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(interviewService.startInterview(userDetails.getUsername(), request));
    }

    @GetMapping("/{sessionId}/next-question")
    public ResponseEntity<QuestionResponse> getNextQuestion(
            @PathVariable String sessionId) {
        log.debug("Getting next question for session: {}", sessionId);
        return ResponseEntity.ok(interviewService.getNextQuestion(sessionId));
    }

    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<QuestionResponse> submitAnswer(
            @PathVariable String sessionId,
            @Valid @RequestBody InterviewAnswerRequest request) {
        log.debug("Submitting answer for session: {}", sessionId);
        return ResponseEntity.ok(interviewService.submitAnswer(sessionId, request));
    }

    @GetMapping("/{sessionId}/status")
    public ResponseEntity<InterviewSessionResponse> getSessionStatus(
            @PathVariable String sessionId) {
        log.debug("Getting status for session: {}", sessionId);
        return ResponseEntity.ok(interviewService.getSessionStatus(sessionId));
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<InterviewSummaryResponse> endInterview(
            @PathVariable String sessionId,
            @Valid @RequestBody InterviewEndRequest request) {
        log.debug("Ending interview session: {}", sessionId);
        return ResponseEntity.ok(interviewService.endInterview(sessionId, request));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<InterviewSessionResponse>> getSessionHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Getting session history for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(interviewService.getSessionHistory(userDetails.getUsername(), page, size));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<InterviewSessionResponse> getSessionDetails(
            @PathVariable String sessionId) {
        log.debug("Getting details for session: {}", sessionId);
        return ResponseEntity.ok(interviewService.getSessionDetails(sessionId));
    }

    @GetMapping("/{sessionId}/questions")
    public ResponseEntity<java.util.List<QuestionResponse>> getAllQuestions(
            @PathVariable String sessionId) {
        log.debug("Getting all questions for session: {}", sessionId);
        return ResponseEntity.ok(interviewService.getAllQuestions(sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> abandonSession(
            @PathVariable String sessionId) {
        log.debug("Abandoning session: {}", sessionId);
        interviewService.abandonSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}/active")
    public ResponseEntity<Boolean> isSessionActive(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(interviewService.isSessionActive(sessionId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<Page<InterviewSessionResponse>> getAdminUserSessionHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Admin fetching session history for userId: {}", userId);
        return ResponseEntity.ok(interviewService.getSessionHistoryByUserId(userId, page, size));
    }
}
