package com.interviewpartner.repository;

import com.interviewpartner.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findBySessionId(Long sessionId);

    Optional<Answer> findByQuestionId(Long questionId);

    List<Answer> findBySessionIdAndIsCorrectTrue(Long sessionId);

    @Query("SELECT COUNT(a) FROM Answer a WHERE a.session.id = :sessionId AND a.isCorrect = true")
    Long countCorrectAnswersBySession(@Param("sessionId") Long sessionId);

    @Query("SELECT AVG(a.score) FROM Answer a WHERE a.session.id = :sessionId")
    Double getAverageScoreBySession(@Param("sessionId") Long sessionId);

    @Query("SELECT AVG(a.communicationScore) FROM Answer a WHERE a.session.user.id = :userId")
    Double getAverageCommunicationScoreByUser(@Param("userId") Long userId);

    @Query("SELECT AVG(a.clarityScore) FROM Answer a WHERE a.session.user.id = :userId")
    Double getAverageClarityScoreByUser(@Param("userId") Long userId);
}