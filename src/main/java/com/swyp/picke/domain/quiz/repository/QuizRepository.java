package com.swyp.picke.domain.quiz.repository;

import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.enums.QuizStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Page<Quiz> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT q FROM Quiz q WHERE q.status = :status AND q.targetDate = :targetDate ORDER BY q.createdAt ASC")
    List<Quiz> findTodayPicks(
            @Param("status") QuizStatus status,
            @Param("targetDate") LocalDate targetDate,
            Pageable pageable
    );

    @Query("""
            SELECT q
              FROM Quiz q
             WHERE q.status = :status
               AND (q.targetDate IS NULL OR q.targetDate <> :targetDate)
             ORDER BY CASE WHEN q.targetDate IS NULL THEN 0 ELSE 1 END,
                      q.targetDate ASC,
                      q.createdAt ASC
            """)
    List<Quiz> findAutoAssignableTodayPicks(
            @Param("status") QuizStatus status,
            @Param("targetDate") LocalDate targetDate,
            Pageable pageable
    );
}
