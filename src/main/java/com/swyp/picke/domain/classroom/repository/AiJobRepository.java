package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.AiJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {

    /** 같은 작업(리포트 1건)은 job 1행을 재사용하고 attempt 만 올린다 (UX04, PRD "같은 job 사용") */
    Optional<AiJob> findByIdempotencyKey(String idempotencyKey);
}
