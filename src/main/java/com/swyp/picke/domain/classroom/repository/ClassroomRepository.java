package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    boolean existsByInviteCode(String inviteCode);

    Optional<Classroom> findByIdAndStatus(Long id, ClassStatus status);

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
}
