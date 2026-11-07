package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.ClassTopicDraft;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassTopicDraftRepository extends JpaRepository<ClassTopicDraft, Long> {

    /** 제안·검증 준비용: 같은 초안에 동시 요청이 와도 쿼터와 진행 상태를 한 번에 하나만 바꾸도록 행 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from ClassTopicDraft d where d.id = :id")
    Optional<ClassTopicDraft> findByIdForUpdate(@Param("id") Long id);
}
