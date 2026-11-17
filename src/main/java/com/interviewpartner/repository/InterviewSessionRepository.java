package com.interviewpartner.repository;

import com.interviewpartner.entity.InterviewSession;
import com.interviewpartner.interview.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    Optional<InterviewSession> findBySessionId(String sessionId);

    List<InterviewSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    Page<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId AND s.status = :status")
    List<InterviewSession> findActiveSessionsByUser(@Param("userId") Long userId,
                                                    @Param("status") SessionStatus status);

    @Query("SELECT s FROM InterviewSession s WHERE s.status = :status AND s.startedAt < :time")
    List<InterviewSession> findSessionsByStatusAndTime(@Param("status") SessionStatus status,
                                                       @Param("time") LocalDateTime time);

    @Query("SELECT COUNT(s) FROM InterviewSession s WHERE s.user.id = :userId")
    Long countTotalSessionsByUser(@Param("userId") Long userId);

    @Query("SELECT AVG(s.score) FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'")
    Double getAverageScoreByUser(@Param("userId") Long userId);

    // ✅ FIXED: This method now exists
    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED' ORDER BY s.completedAt DESC")
    List<InterviewSession> findCompletedSessionsByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId AND s.completedAt BETWEEN :startDate AND :endDate")
    List<InterviewSession> findByUserIdAndCompletedAtBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}