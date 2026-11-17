package com.interviewpartner.service.analytics.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.interviewpartner.dto.response.*;
import com.interviewpartner.entity.*;
import com.interviewpartner.repository.*;
import com.interviewpartner.service.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final PerformanceTrendRepository trendRepository;
    private final InterviewSessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void updateSessionAnalytics(InterviewSession session) {
        log.debug("Updating analytics for session: {}", session.getSessionId());

        List<Answer> answers = answerRepository.findBySessionId(session.getId());
        if (answers.isEmpty()) {
            return;
        }

        double avgTime = answers.stream()
                .filter(a -> a.getTimeSpentSeconds() != null)
                .mapToDouble(Answer::getTimeSpentSeconds)
                .average()
                .orElse(0.0);

        Map<String, List<Double>> categoryScores = new HashMap<>();
        for (Answer answer : answers) {
            Question question = answer.getQuestion();
            String category = question.getCategory() != null ? question.getCategory() : "General";
            categoryScores.computeIfAbsent(category, k -> new ArrayList<>())
                    .add(answer.getScore() != null ? answer.getScore() : 0.0);
        }

        for (Map.Entry<String, List<Double>> entry : categoryScores.entrySet()) {
            String category = entry.getKey();
            List<Double> scores = entry.getValue();
            double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            Analytics analytics = analyticsRepository.findBySessionIdAndCategory(session.getId(), category)
                    .orElseGet(() -> Analytics.builder()
                            .user(session.getUser())
                            .session(session)
                            .category(category)
                            .build());

            analytics.setScore(avgScore * 100);
            analytics.setQuestionsAttempted(scores.size());
            analytics.setQuestionsCorrect((int) scores.stream().filter(s -> s > 0.5).count());
            analytics.setAverageTimePerQuestion(avgTime);
            analytics.setDifficultyLevel(session.getDifficulty());
            analytics.setInterviewMode(session.getMode());
            analytics.setRecordedAt(LocalDateTime.now());

            analyticsRepository.save(analytics);
        }
    }

    @Override
    @Transactional
    public void finalizeSessionAnalytics(InterviewSession session) {
        log.debug("Finalizing analytics for session: {}", session.getSessionId());
        
        sessionRepository.save(session);

        List<Answer> answers = answerRepository.findBySessionId(session.getId());
        double avgTime = answers.stream()
                .filter(a -> a.getTimeSpentSeconds() != null)
                .mapToDouble(Answer::getTimeSpentSeconds)
                .average()
                .orElse(0.0);

        String feedbackSummaryText = session.getFeedbackSummary();
        String strengthsJson = null;
        String weaknessesJson = null;
        String recsJson = null;

        if (feedbackSummaryText != null && feedbackSummaryText.trim().startsWith("{")) {
            try {
                JsonNode json = objectMapper.readTree(feedbackSummaryText);
                if (json.has("strengths")) {
                    strengthsJson = objectMapper.writeValueAsString(json.get("strengths"));
                }
                if (json.has("weaknesses")) {
                    weaknessesJson = objectMapper.writeValueAsString(json.get("weaknesses"));
                }
                if (json.has("suggestions")) {
                    recsJson = objectMapper.writeValueAsString(json.get("suggestions"));
                }
            } catch (Exception e) {
                log.warn("Failed to parse feedback summary for final analytics: {}", e.getMessage());
            }
        }

        List<Analytics> sessionAnalytics = analyticsRepository.findByUserIdOrderByRecordedAtDesc(session.getUser().getId())
                .stream()
                .filter(a -> a.getSession() != null && a.getSession().getId().equals(session.getId()))
                .collect(Collectors.toList());

        for (Analytics analytics : sessionAnalytics) {
            analytics.setCommunicationScore(session.getCommunicationScore());
            analytics.setClarityScore(session.getClarityScore());
            analytics.setCorrectnessScore(session.getCorrectnessScore());
            analytics.setScore(session.getScore());
            analytics.setAverageTimePerQuestion(avgTime);
            if (strengthsJson != null) analytics.setStrengths(strengthsJson);
            if (weaknessesJson != null) analytics.setWeaknesses(weaknessesJson);
            if (recsJson != null) analytics.setRecommendations(recsJson);
            analyticsRepository.save(analytics);
        }

        trackDailyPerformance(session.getUser().getEmail());
    }

    @Override
    public AnalyticsResponse getUserAnalytics(String email) {
        log.debug("Getting user analytics for: {}", email);

        User user = getUserByEmail(email);

        List<InterviewSession> sessions = sessionRepository.findCompletedSessionsByUser(
                user.getId(), PageRequest.of(0, 100));

        if (sessions.isEmpty()) {
            return buildEmptyAnalytics();
        }

        OverallStats overallStats = calculateOverallStats(sessions);
        List<CategoryStats> categoryStats = calculateCategoryStats(user);
        
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        Optional<InterviewSession> recentCompleted = sessions.stream()
                .filter(s -> s.getFeedbackSummary() != null && s.getFeedbackSummary().trim().startsWith("{"))
                .findFirst();

        if (recentCompleted.isPresent()) {
            try {
                JsonNode json = objectMapper.readTree(recentCompleted.get().getFeedbackSummary());
                if (json.has("strengths")) {
                    for (JsonNode n : json.get("strengths")) strengths.add(n.asText());
                }
                if (json.has("weaknesses")) {
                    for (JsonNode n : json.get("weaknesses")) weaknesses.add(n.asText());
                }
                if (json.has("suggestions")) {
                    for (JsonNode n : json.get("suggestions")) recommendations.add(n.asText());
                }
            } catch (Exception e) {
                log.warn("Failed to parse recent session feedback JSON for analytics dashboard: {}", e.getMessage());
            }
        }

        if (strengths.isEmpty()) strengths = detectStrengths(categoryStats);
        if (weaknesses.isEmpty()) weaknesses = detectWeaknesses(categoryStats);
        if (recommendations.isEmpty()) recommendations = generateRecommendations(weaknesses, categoryStats);

        ProgressData progressData = getProgressData(user);

        String strongestCategory = categoryStats.stream()
                .max(Comparator.comparing(CategoryStats::getScore))
                .map(CategoryStats::getCategory)
                .orElse("N/A");

        String weakestCategory = categoryStats.stream()
                .min(Comparator.comparing(CategoryStats::getScore))
                .map(CategoryStats::getCategory)
                .orElse("N/A");

        return AnalyticsResponse.builder()
                .overallStats(overallStats)
                .categoryStats(categoryStats)
                .categoryScores(categoryStats.stream()
                        .collect(Collectors.toMap(CategoryStats::getCategory, CategoryStats::getScore)))
                .strengths(strengths)
                .weaknesses(weaknesses)
                .recommendations(recommendations)
                .strongestCategory(strongestCategory)
                .weakestCategory(weakestCategory)
                .consistencyScore(calculateConsistency(categoryStats))
                .progressData(progressData)
                .build();
    }

    @Override
    public Map<String, Double> getCategoryScores(String email) {
        User user = getUserByEmail(email);
        return analyticsRepository.findAverageScoreByCategory(user.getId())
                .stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> (Double) obj[1]
                ));
    }

    @Override
    public AnalyticsResponse getTrendData(String email, int days) {
        User user = getUserByEmail(email);
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<PerformanceTrend> trends = trendRepository.findByUserIdAndTrendDateAfter(
                user.getId(), startDate);

        List<TrendData> trendData = trends.stream()
                .map(t -> TrendData.builder()
                        .date(t.getTrendDate().toString())
                        .score(t.getOverallScore())
                        .communicationScore(t.getCommunicationScore())
                        .clarityScore(t.getClarityScore())
                        .correctnessScore(t.getCorrectnessScore())
                        .interviews(t.getTotalInterviews())
                        .build())
                .collect(Collectors.toList());

        return AnalyticsResponse.builder()
                .progressData(ProgressData.builder()
                        .trends(trendData)
                        .improvementOverTime(calculateOverallImprovement(trends))
                        .build())
                .build();
    }

    @Override
    public Map<String, Double> getWeakTopics(String email) {
        User user = getUserByEmail(email);
        Map<String, Double> weakTopics = new HashMap<>();
        List<Object[]> results = analyticsRepository.findWeakTopics(user.getId(), 50.0); // Threshold changed to 50%

        for (Object[] result : results) {
            weakTopics.put((String) result[0], (Double) result[1]);
        }
        return weakTopics;
    }

    @Override
    public Map<String, Object> getRecommendations(String email) {
        User user = getUserByEmail(email);
        Map<String, Object> recommendations = new HashMap<>();
        Map<String, Double> weakTopics = getWeakTopics(email);

        List<String> suggestions = new ArrayList<>();
        for (Map.Entry<String, Double> entry : weakTopics.entrySet()) {
            String topic = entry.getKey();
            Double score = entry.getValue();
            suggestions.add(String.format("Improve your %s skills (current score: %.1f%%)", topic, score));
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Continue practicing with mock interviews to maintain your skills.");
        }

        recommendations.put("weakTopics", weakTopics);
        recommendations.put("suggestions", suggestions);
        recommendations.put("priorityArea", weakTopics.isEmpty() ? "None" :
                weakTopics.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None"));

        return recommendations;
    }

    @Override
    @Transactional
    public void trackDailyPerformance(String email) {
        User user = getUserByEmail(email);
        LocalDate today = LocalDate.now();

        Optional<PerformanceTrend> existingTrend = trendRepository
                .findByUserIdAndTrendDate(user.getId(), today);

        if (existingTrend.isPresent()) {
            PerformanceTrend trend = existingTrend.get();
            updateTrendFromSessions(trend, user);
            trendRepository.save(trend);
        } else {
            PerformanceTrend trend = PerformanceTrend.builder()
                    .user(user)
                    .trendDate(today)
                    .build();
            updateTrendFromSessions(trend, user);
            trendRepository.save(trend);
        }
    }

    @Override
    public Map<String, Object> calculateImprovement(String email) {
        User user = getUserByEmail(email);
        List<PerformanceTrend> trends = trendRepository.findByUserIdOrderByTrendDateDesc(user.getId());
        Map<String, Object> improvement = new HashMap<>();

        if (trends.size() < 2) {
            improvement.put("message", "Not enough data to calculate improvement");
            improvement.put("improvementRate", 0.0);
            return improvement;
        }

        PerformanceTrend latest = trends.get(0);
        PerformanceTrend earliest = trends.get(trends.size() - 1);

        double improvementRate = ((latest.getOverallScore() - earliest.getOverallScore()) /
                (earliest.getOverallScore() + 0.01)) * 100;

        improvement.put("improvementRate", improvementRate);
        improvement.put("improvementTrend", improvementRate > 0 ? "IMPROVING" : "DECLINING");
        improvement.put("totalInterviews", latest.getTotalInterviews());
        improvement.put("currentScore", latest.getOverallScore());
        improvement.put("previousScore", earliest.getOverallScore());

        return improvement;
    }

    // ============ Private Helper Methods ============

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private OverallStats calculateOverallStats(List<InterviewSession> sessions) {
        List<Double> scores = sessions.stream()
                .map(InterviewSession::getScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (scores.isEmpty()) {
            return OverallStats.builder()
                    .averageScore(0.0)
                    .maxScore(0.0)
                    .minScore(0.0)
                    .totalInterviews(0)
                    .build();
        }

        double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double min = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

        int totalQuestions = sessions.stream().mapToInt(InterviewSession::getTotalQuestions).sum();
        int correctAnswers = sessions.stream().mapToInt(InterviewSession::getCorrectAnswers).sum();

        double avgDuration = sessions.stream()
                .filter(s -> s.getStartedAt() != null && s.getCompletedAt() != null)
                .mapToDouble(s -> Duration.between(s.getStartedAt(), s.getCompletedAt()).toMinutes())
                .average()
                .orElse(0.0);

        double avgTimePerQuestion = sessions.stream()
                .flatMap(s -> answerRepository.findBySessionId(s.getId()).stream())
                .filter(a -> a.getTimeSpentSeconds() != null)
                .mapToDouble(Answer::getTimeSpentSeconds)
                .average()
                .orElse(45.0);

        int dayStreak = calculateDayStreak(sessions);

        return OverallStats.builder()
                .averageScore(avg)
                .maxScore(max)
                .minScore(min)
                .communicationScore(sessions.stream()
                        .map(InterviewSession::getCommunicationScore)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0))
                .clarityScore(sessions.stream()
                        .map(InterviewSession::getClarityScore)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0))
                .correctnessScore(sessions.stream()
                        .map(InterviewSession::getCorrectnessScore)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0))
                .totalInterviews(sessions.size())
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .accuracyRate(totalQuestions > 0 ? (double) correctAnswers / totalQuestions * 100 : 0.0)
                .averageTimePerQuestion(avgTimePerQuestion)
                .averageInterviewDuration(avgDuration)
                .dayStreak(dayStreak)
                .build();
    }

    private int calculateDayStreak(List<InterviewSession> completedSessions) {
        if (completedSessions.isEmpty()) {
            return 0;
        }

        List<LocalDate> completionDates = completedSessions.stream()
                .filter(s -> s.getCompletedAt() != null)
                .map(s -> s.getCompletedAt().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        if (completionDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        LocalDate mostRecent = completionDates.get(0);
        if (!mostRecent.equals(today) && !mostRecent.equals(yesterday)) {
            return 0;
        }

        int streak = 1;
        LocalDate expectedDate = mostRecent.minusDays(1);

        for (int i = 1; i < completionDates.size(); i++) {
            LocalDate date = completionDates.get(i);
            if (date.equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else if (date.isAfter(expectedDate)) {
                continue;
            } else {
                break;
            }
        }

        return streak;
    }

    private List<CategoryStats> calculateCategoryStats(User user) {
        List<Object[]> results = analyticsRepository.findCategoryStats(user.getId());
        List<CategoryStats> stats = new ArrayList<>();

        for (Object[] result : results) {
            String category = (String) result[0];
            Double avgScore = (Double) result[1];
            Long attempted = (Long) result[2];
            Long correct = (Long) result[3];

            stats.add(CategoryStats.builder()
                    .category(category)
                    .score(avgScore != null ? avgScore : 0.0)
                    .questionsAttempted(attempted != null ? attempted.intValue() : 0)
                    .questionsCorrect(correct != null ? correct.intValue() : 0)
                    .accuracy(attempted != null && attempted > 0 ?
                            (double) correct / attempted * 100 : 0.0)
                    .trend("STABLE")
                    .build());
        }
        return stats;
    }

    private List<String> detectStrengths(List<CategoryStats> categoryStats) {
        return categoryStats.stream()
                .filter(c -> c.getScore() >= 70)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(3)
                .map(c -> c.getCategory() + " (" + String.format("%.1f", c.getScore()) + "%)")
                .collect(Collectors.toList());
    }

    private List<String> detectWeaknesses(List<CategoryStats> categoryStats) {
        return categoryStats.stream()
                .filter(c -> c.getScore() < 50)
                .sorted((a, b) -> Double.compare(a.getScore(), b.getScore()))
                .limit(3)
                .map(c -> c.getCategory() + " (" + String.format("%.1f", c.getScore()) + "%)")
                .collect(Collectors.toList());
    }

    private List<String> generateRecommendations(List<String> weaknesses, List<CategoryStats> categoryStats) {
        List<String> recommendations = new ArrayList<>();

        if (weaknesses.isEmpty()) {
            recommendations.add("Great job! Keep practicing to maintain your skills.");
            return recommendations;
        }

        for (String weakness : weaknesses) {
            String category = weakness.split("\\(")[0].trim();
            recommendations.add("Focus on improving your " + category + " skills. Practice more questions in this area.");
        }

        recommendations.add("Review fundamental concepts in your weakest areas.");
        recommendations.add("Try to complete at least 5 practice questions daily.");
        return recommendations;
    }

    private Double calculateConsistency(List<CategoryStats> categoryStats) {
        if (categoryStats.isEmpty()) return 0.0;

        List<Double> scores = categoryStats.stream()
                .map(CategoryStats::getScore)
                .collect(Collectors.toList());

        double mean = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = scores.stream()
                .mapToDouble(s -> Math.pow(s - mean, 2))
                .average()
                .orElse(0.0);

        return 100.0 - (variance / 100.0);
    }

    private ProgressData getProgressData(User user) {
        List<PerformanceTrend> trends = trendRepository.findByUserIdOrderByTrendDateDesc(
                user.getId(), PageRequest.of(0, 30));

        List<TrendData> trendData = trends.stream()
                .map(t -> TrendData.builder()
                        .date(t.getTrendDate().toString())
                        .score(t.getOverallScore())
                        .communicationScore(t.getCommunicationScore())
                        .clarityScore(t.getClarityScore())
                        .correctnessScore(t.getCorrectnessScore())
                        .interviews(t.getTotalInterviews())
                        .build())
                .collect(Collectors.toList());

        return ProgressData.builder()
                .trends(trendData)
                .improvementOverTime(calculateOverallImprovement(trends))
                .improvementRate(trends.size() > 1 ?
                        String.format("%.1f%%", (trends.get(0).getOverallScore() -
                                trends.get(trends.size() - 1).getOverallScore()) /
                                                (trends.get(trends.size() - 1).getOverallScore() + 0.01) * 100) :
                        "0.0%")
                .build();
    }

    private Double calculateOverallImprovement(List<PerformanceTrend> trends) {
        if (trends.size() < 2) return 0.0;

        double first = trends.get(trends.size() - 1).getOverallScore();
        double last = trends.get(0).getOverallScore();
        return ((last - first) / (first + 0.01)) * 100;
    }

    private void updateTrendFromSessions(PerformanceTrend trend, User user) {
        LocalDate date = trend.getTrendDate();

        List<InterviewSession> sessions = sessionRepository.findByUserIdAndCompletedAtBetween(
                user.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        if (sessions.isEmpty()) {
            trend.setOverallScore(0.0);
            trend.setTotalInterviews(0);
            return;
        }

        double avgScore = sessions.stream()
                .mapToDouble(s -> s.getScore() != null ? s.getScore() : 0.0)
                .average()
                .orElse(0.0);

        double avgCommunication = sessions.stream()
                .mapToDouble(s -> s.getCommunicationScore() != null ? s.getCommunicationScore() : 0.0)
                .average()
                .orElse(0.0);

        double avgClarity = sessions.stream()
                .mapToDouble(s -> s.getClarityScore() != null ? s.getClarityScore() : 0.0)
                .average()
                .orElse(0.0);

        double avgCorrectness = sessions.stream()
                .mapToDouble(s -> s.getCorrectnessScore() != null ? s.getCorrectnessScore() : 0.0)
                .average()
                .orElse(0.0);

        trend.setOverallScore(avgScore);
        trend.setCommunicationScore(avgCommunication);
        trend.setClarityScore(avgClarity);
        trend.setCorrectnessScore(avgCorrectness);
        trend.setTotalInterviews(sessions.size());
    }

    private AnalyticsResponse buildEmptyAnalytics() {
        return AnalyticsResponse.builder()
                .overallStats(OverallStats.builder()
                        .averageScore(0.0)
                        .maxScore(0.0)
                        .minScore(0.0)
                        .totalInterviews(0)
                        .totalQuestions(0)
                        .correctAnswers(0)
                        .accuracyRate(0.0)
                        .averageInterviewDuration(0.0)
                        .dayStreak(0)
                        .build())
                .categoryStats(Collections.emptyList())
                .categoryScores(Collections.emptyMap())
                .strengths(Collections.singletonList("Complete your first interview to get insights!"))
                .weaknesses(Collections.singletonList("No data available yet"))
                .recommendations(Collections.singletonList("Start a mock interview to begin tracking your progress"))
                .strongestCategory("N/A")
                .weakestCategory("N/A")
                .consistencyScore(0.0)
                .progressData(ProgressData.builder()
                        .trends(Collections.emptyList())
                        .improvementOverTime(0.0)
                        .improvementRate("0.0%")
                        .build())
                .build();
    }
}