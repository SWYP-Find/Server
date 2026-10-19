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
