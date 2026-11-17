package com.interviewpartner.repository;

import com.interviewpartner.entity.PerformanceTrend;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PerformanceTrendRepository extends JpaRepository<PerformanceTrend, Long> {

    List<PerformanceTrend> findByUserIdOrderByTrendDateDesc(Long userId);

    List<PerformanceTrend> findByUserIdOrderByTrendDateDesc(Long userId, Pageable pageable);

    Optional<PerformanceTrend> findByUserIdAndTrendDate(Long userId, LocalDate date);

    List<PerformanceTrend> findByUserIdAndTrendDateAfter(Long userId, LocalDate date);

    @Query("SELECT t FROM PerformanceTrend t WHERE t.user.id = :userId AND t.trendDate BETWEEN :startDate AND :endDate ORDER BY t.trendDate ASC")
    List<PerformanceTrend> findByUserIdAndTrendDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(t.overallScore) FROM PerformanceTrend t WHERE t.user.id = :userId AND t.trendDate >= :startDate")
    Double getAverageScoreSince(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);

    void deleteByUserId(Long userId);
}