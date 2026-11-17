package com.interviewpartner.service;

import com.interviewpartner.dto.request.InterviewAnswerRequest;
import com.interviewpartner.dto.request.InterviewEndRequest;
import com.interviewpartner.dto.request.InterviewStartRequest;
import com.interviewpartner.dto.response.InterviewSessionResponse;
import com.interviewpartner.dto.response.InterviewSummaryResponse;
import com.interviewpartner.dto.response.QuestionResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InterviewService {

    InterviewSessionResponse startInterview(String email, InterviewStartRequest request);
    QuestionResponse getNextQuestion(String sessionId);
    QuestionResponse submitAnswer(String sessionId, InterviewAnswerRequest request);
    InterviewSessionResponse getSessionStatus(String sessionId);
    InterviewSummaryResponse endInterview(String sessionId, InterviewEndRequest request);
    Page<InterviewSessionResponse> getSessionHistory(String email, int page, int size);
    Page<InterviewSessionResponse> getSessionHistoryByUserId(Long userId, int page, int size);
    InterviewSessionResponse getSessionDetails(String sessionId);
    List<QuestionResponse> getAllQuestions(String sessionId);
    void abandonSession(String sessionId);
    boolean isSessionActive(String sessionId);
}