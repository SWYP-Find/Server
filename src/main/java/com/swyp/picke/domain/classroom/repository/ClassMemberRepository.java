package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {

    Optional<ClassMember> findByClassroomIdAndUserIdAndStatus(Long classroomId, Long userId, ClassMemberStatus status);

    /** 참여용: 상태와 무관하게 이 유저의 멤버 행 (UK(class_id, user_id) 라 최대 1건) */
    Optional<ClassMember> findByClassroomIdAndUserId(Long classroomId, Long userId);

    Optional<ClassMember> findByIdAndClassroomId(Long id, Long classroomId);

    /** 의견·댓글 작성자 표시 이름 일괄 조회용. 나간 멤버도 행이 남아 있어 이름을 그대로 쓴다 */
    List<ClassMember> findByClassroomIdAndUserIdIn(Long classroomId, Collection<Long> userIds);

    /** 동명이인 검사: ACTIVE 멤버끼리만 이름이 유일하면 된다 */
    boolean existsByClassroomIdAndDisplayNameAndStatus(Long classroomId, String displayName, ClassMemberStatus status);

    /** 명단용. 운영자 행이 먼저, 그다음 참여 순 */
    @Query("""
            select m from ClassMember m
            join fetch m.user
            where m.classroom.id = :classroomId
              and m.status = com.swyp.picke.domain.classroom.enums.ClassMemberStatus.ACTIVE
            order by case when m.role = com.swyp.picke.domain.classroom.enums.ClassMemberRole.OWNER then 0 else 1 end,
                     m.joinedAt asc, m.id asc
            """)
    List<ClassMember> findActiveMembers(@Param("classroomId") Long classroomId);

    /** 정원 판정용. 좌석을 쥐고 있는 인원(seatHeld = true) 수 */
    long countByClassroomIdAndSeatHeldTrue(Long classroomId);

    /** 목록 화면용: 여러 클래스의 좌석 소비 인원을 한 번에 */
    @Query("""
            select m.classroom.id, count(m) from ClassMember m
            where m.classroom.id in :classroomIds and m.seatHeld = true
            group by m.classroom.id
            """)
    List<Object[]> countSeatsHeldByClassroomIds(@Param("classroomIds") Collection<Long> classroomIds);
}
