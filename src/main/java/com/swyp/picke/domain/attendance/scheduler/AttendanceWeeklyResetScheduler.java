package com.swyp.picke.domain.attendance.scheduler;

import com.swyp.picke.domain.attendance.entity.AttendanceStreak;
import com.swyp.picke.domain.attendance.repository.AttendanceStreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 매주 월요일 00:00 (KST) 에 연속 출석 카운트를 초기화한다.
 *
 * - current_streak, is_streak_achieved 를 0 / false 로 초기화
 * - streak_week_start 를 이번 주 월요일로 업데이트
 * - max_streak 는 유지 (최장 기록 보존)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceWeeklyResetScheduler {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final AttendanceStreakRepository attendanceStreakRepository;

    @Scheduled(cron = "0 0 0 ? * MON", zone = "Asia/Seoul")
    @Transactional
    public void resetWeeklyStreak() {
        LocalDate thisMonday = LocalDate.now(SEOUL_ZONE)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        log.info("[AttendanceWeeklyReset] start weekStart={}", thisMonday);

        List<AttendanceStreak> streaks = attendanceStreakRepository.findAll();
        streaks.forEach(streak -> streak.initWeek(thisMonday));

        log.info("[AttendanceWeeklyReset] end count={}", streaks.size());
    }
}
