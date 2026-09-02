package com.swyp.picke.domain.attendance.repository;

import com.swyp.picke.domain.attendance.entity.AttendanceRecord;
import com.swyp.picke.domain.user.repository.projection.DailyUserCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    boolean existsByUserIdAndAttendedDate(Long userId, LocalDate attendedDate);

    List<AttendanceRecord> findByUserIdAndAttendedDateBetween(Long userId, LocalDate from, LocalDate to);

    Page<AttendanceRecord> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    Optional<AttendanceRecord> findTopByUserIdOrderByAttendedDateDesc(Long userId);

    @Query("SELECT ar.attendedDate, COUNT(DISTINCT ar.user.id) FROM AttendanceRecord ar " +
            "WHERE ar.attendedDate BETWEEN :from AND :to " +
            "GROUP BY ar.attendedDate ORDER BY ar.attendedDate")
    List<Object[]> countAttendanceGroupByDate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * 어드민 대시보드용: from~to 구간의 일자별 출석 인원 수. 출석이 없는 날짜도 0으로 채워서 반환한다.
     */
    @Query(value = """
            SELECT gs::date AS activity_date, COUNT(ar.user_id) AS count
            FROM generate_series(CAST(:from AS timestamp), CAST(:to AS timestamp), interval '1 day') AS gs
            LEFT JOIN attendance_records ar ON ar.attended_date = gs::date
            GROUP BY gs::date
            ORDER BY gs::date
            """, nativeQuery = true)
    List<DailyUserCount> findDailyAttendanceCounts(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
