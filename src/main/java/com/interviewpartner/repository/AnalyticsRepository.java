package com.interviewpartner.repository;

import com.interviewpartner.entity.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {

    @Query("SELECT a FROM Analytics a WHERE a.user.id = :userId ORDER BY a.recordedAt DESC")
    List<Analytics> findByUserIdOrderByRecordedAtDesc(@Param("userId") Long userId);

    @Query("SELECT a.category, AVG(a.score) FROM Analytics a WHERE a.user.id = :userId GROUP BY a.category")
    List<Object[]> findAverageScoreByCategory(@Param("userId") Long userId);

    @Query("SELECT a.category, AVG(a.score) FROM Analytics a WHERE a.user.id = :userId GROUP BY a.category HAVING AVG(a.score) < :threshold")
    List<Object[]> findWeakTopics(@Param("userId") Long userId, @Param("threshold") double threshold);

    @Query("SELECT a.category, AVG(a.score), COUNT(a.id), SUM(CASE WHEN a.questionsCorrect > 0 THEN 1 ELSE 0 END) " +
            "FROM Analytics a WHERE a.user.id = :userId GROUP BY a.category")
    List<Object[]> findCategoryStats(@Param("userId") Long userId);

    @Query("SELECT a.category, AVG(a.score) FROM Analytics a WHERE a.user.id = :userId AND a.interviewMode = :mode GROUP BY a.category")
    List<Object[]> findCategoryStatsByMode(@Param("userId") Long userId, @Param("mode") String mode);

    @Query("SELECT a FROM Analytics a WHERE a.session.id = :sessionId AND a.category = :category")
    Optional<Analytics> findBySessionIdAndCategory(@Param("sessionId") Long sessionId, @Param("category") String category);

    void deleteByUserId(Long userId);
}