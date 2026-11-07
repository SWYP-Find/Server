package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.AiJob;
import com.swyp.picke.domain.classroom.enums.AiJobType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {

    /** 같은 작업(리포트 1건)은 job 1행을 재사용하고 attempt 만 올린다 (UX04, PRD "같은 job 사용") */
    Optional<AiJob> findByIdempotencyKey(String idempotencyKey);

    /** 계정별 분당 요청 제한용. 계정당 사용 중 이용권은 1개라 이용권 기준으로 센다 (PRD §10) */
    long countByTicketIdAndTypeAndStartedAtAfter(Long ticketId, AiJobType type, LocalDateTime after);
}
