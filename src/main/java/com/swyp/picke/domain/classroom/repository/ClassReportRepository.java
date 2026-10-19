package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.ClassReport;
import com.swyp.picke.domain.classroom.enums.ClassReportStatus;
import com.swyp.picke.domain.classroom.enums.ClassReportType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * class_reports 는 (class_id, member_id, type) 으로 유일하다. CLASS_SUMMARY 는 member null.
 */
public interface ClassReportRepository extends JpaRepository<ClassReport, Long> {

    /** 클래스 요약(인사이트) 리포트. 이 행의 존재 = 마감 스냅샷이 이미 만들어졌다는 뜻 */
    Optional<ClassReport> findByClassroomIdAndTypeAndMemberIsNull(Long classroomId, ClassReportType type);

    Optional<ClassReport> findByClassroomIdAndMemberIdAndType(Long classroomId, Long memberId, ClassReportType type);

    boolean existsByClassroomIdAndType(Long classroomId, ClassReportType type);

    /** 스케줄러 처리 대상: 생성 대기 중인 리포트를 오래된 순으로 */
    @Query("""
            select r from ClassReport r
            join fetch r.classroom c
            left join fetch r.member m
            where r.status = :status
            order by r.id asc
            """)
    List<ClassReport> findByStatusOrderByIdAsc(@Param("status") ClassReportStatus status, Pageable pageable);
}
