package com.swyp.picke.domain.attendance.repository;

import com.swyp.picke.domain.attendance.entity.AttendanceStreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceStreakRepository extends JpaRepository<AttendanceStreak, Long> {

    Optional<AttendanceStreak> findByUserId(Long userId);
}
