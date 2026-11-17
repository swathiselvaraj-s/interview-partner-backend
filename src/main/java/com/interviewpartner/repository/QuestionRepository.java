package com.interviewpartner.repository;

import com.interviewpartner.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySessionIdOrderBySequenceNumberAsc(Long sessionId);

    Optional<Question> findBySessionIdAndSequenceNumber(Long sessionId, Integer sequenceNumber);

    @Query("SELECT q FROM Question q WHERE q.session.id = :sessionId AND q.sequenceNumber = :sequenceNumber")
    Optional<Question> findQuestionBySessionAndSequence(@Param("sessionId") Long sessionId,
                                                        @Param("sequenceNumber") Integer sequenceNumber);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.session.id = :sessionId")
    Integer countQuestionsBySession(@Param("sessionId") Long sessionId);

    @Query("SELECT q FROM Question q WHERE q.session.user.id = :userId AND q.category = :category")
    List<Question> findQuestionsByUserAndCategory(@Param("userId") Long userId,
                                                  @Param("category") String category);
}