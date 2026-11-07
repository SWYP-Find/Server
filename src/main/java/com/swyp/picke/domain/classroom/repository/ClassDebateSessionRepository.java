package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.ClassDebateSession;
import com.swyp.picke.domain.classroom.enums.ClassDebateStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassDebateSessionRepository extends JpaRepository<ClassDebateSession, Long> {

    Optional<ClassDebateSession> findByClassroomIdAndMemberId(Long classroomId, Long memberId);

    /** 반박·재시도용: 두 기기에서 동시에 다음 턴을 만들지 못하도록 세션 행 잠금 (PRD §8) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ClassDebateSession s where s.classroom.id = :classId and s.member.id = :memberId")
    Optional<ClassDebateSession> findForUpdate(@Param("classId") Long classId, @Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ClassDebateSession s where s.id = :id")
    Optional<ClassDebateSession> findByIdForUpdate(@Param("id") Long id);

    boolean existsByClassroomIdAndMemberIdAndStatus(Long classroomId, Long memberId, ClassDebateStatus status);
}
