package com.swyp.picke.domain.attendance.repository;

import com.swyp.picke.domain.attendance.entity.AttendanceRecord;
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
}
