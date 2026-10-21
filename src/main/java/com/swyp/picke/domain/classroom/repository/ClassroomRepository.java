package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    boolean existsByInviteCode(String inviteCode);

    /** 같은 주제 초안으로 만든 클래스가 있는지 (삭제된 클래스 포함, PRD §16 draft_id 고유) */
    boolean existsByTopicDraftId(Long topicDraftId);

    Optional<Classroom> findByIdAndStatus(Long id, ClassStatus status);

    /**
     * 참여용: 정원 검사와 멤버 생성이 한 트랜잭션에서 직렬화되도록 클래스 행을 잠근다(SELECT ... FOR UPDATE).
     * 마지막 1석을 두 사람이 동시에 신청하면 한 명만 들어간다 (PRD F06).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Classroom c where c.inviteCode = :inviteCode and c.status = :status")
    Optional<Classroom> findByInviteCodeAndStatusForUpdate(@Param("inviteCode") String inviteCode,
                                                          @Param("status") ClassStatus status);

    /**
     * 내 클래스 목록: 내가 운영자인 클래스 + 내가 ACTIVE 멤버인 클래스.
     * 운영자는 ownerParticipates=false 면 멤버 행이 없어서 owner 조건을 따로 둔다.
     */
    @Query("""
            select c from Classroom c
            join fetch c.battle
            join fetch c.ticket
            where c.status = com.swyp.picke.domain.classroom.enums.ClassStatus.ACTIVE
              and (c.owner.id = :userId
                   or exists (select 1 from ClassMember m
                              where m.classroom = c and m.user.id = :userId
                                and m.status = com.swyp.picke.domain.classroom.enums.ClassMemberStatus.ACTIVE))
            order by c.createdAt desc
            """)
    List<Classroom> findMyClasses(@Param("userId") Long userId);

    /**
     * 리포트 스케줄러용: 마감(effective_close_at = min(deadline, 이용권 종료))이 지났는데 아직 CLASS_SUMMARY 리포트 행이 없는 클래스.
     * 리포트 행 생성이 마감 1회 처리 마커 역할을 한다 (UK(class_id, member_id, type)).
     */
    @Query("""
            select c from Classroom c
            join fetch c.ticket t
            join fetch c.battle
            where c.status = com.swyp.picke.domain.classroom.enums.ClassStatus.ACTIVE
              and ((c.deadline is not null and c.deadline <= :now)
                   or (t.expiresAt is not null and t.expiresAt <= :now)
                   or t.status = com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED)
              and not exists (select 1 from ClassReport r
                              where r.classroom = c
                                and r.type = com.swyp.picke.domain.classroom.enums.ClassReportType.CLASS_SUMMARY)
            order by c.id asc
            """)
    List<Classroom> findClosedWithoutReports(@Param("now") LocalDateTime now);
}
