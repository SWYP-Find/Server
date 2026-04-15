package com.swyp.picke.domain.poll.repository;

import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.enums.PollStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollRepository extends JpaRepository<Poll, Long> {
    Page<Poll> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Poll p WHERE p.status = :status AND p.targetDate = :targetDate ORDER BY p.createdAt ASC")
    List<Poll> findTodayPicks(
            @Param("status") PollStatus status,
            @Param("targetDate") LocalDate targetDate,
            Pageable pageable
    );

    @Query("""
            SELECT p
              FROM Poll p
             WHERE p.status = :status
               AND (p.targetDate IS NULL OR p.targetDate <> :targetDate)
             ORDER BY CASE WHEN p.targetDate IS NULL THEN 0 ELSE 1 END,
                      p.targetDate ASC,
                      p.createdAt ASC
            """)
    List<Poll> findAutoAssignableTodayPicks(
            @Param("status") PollStatus status,
            @Param("targetDate") LocalDate targetDate,
            Pageable pageable
    );
}
