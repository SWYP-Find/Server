package com.swyp.picke.domain.attendance.service;

import com.swyp.picke.domain.attendance.dto.response.AttendanceResponse.CheckResponse;
import com.swyp.picke.domain.attendance.dto.response.AttendanceResponse.SummaryResponse;
import com.swyp.picke.domain.attendance.dto.response.AttendanceResponse.WeeklyResponse;
import com.swyp.picke.domain.attendance.dto.response.AttendanceResponse.WeeklyResponse.DayAttendance;
import com.swyp.picke.domain.attendance.entity.AttendanceRecord;
import com.swyp.picke.domain.attendance.entity.AttendanceStreak;
import com.swyp.picke.domain.attendance.enums.AttendanceStatus;
import com.swyp.picke.domain.attendance.repository.AttendanceRecordRepository;
import com.swyp.picke.domain.attendance.repository.AttendanceStreakRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int STREAK_BONUS_THRESHOLD = 7;
    private static final int STREAK_REWARD_POINTS = CreditType.ATTENDANCE_STREAK.getDefaultAmount();

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceStreakRepository attendanceStreakRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final CreditService creditService;

    @Transactional
    public CheckResponse check() {
        User user = userService.findCurrentUser();
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        if (attendanceRecordRepository.existsByUserIdAndAttendedDate(user.getId(), today)) {
            throw new CustomException(ErrorCode.ATTENDANCE_ALREADY_CHECKED);
        }

        int dailyPoints = CreditType.TODAY_CREDIT.getDefaultAmount();
        AttendanceRecord record = AttendanceRecord.builder()
                .user(user)
                .attendedDate(today)
                .pointsEarned(dailyPoints)
                .build();
        attendanceRecordRepository.save(record);

        Long referenceId = toDateLong(today);
        creditService.addCredit(user.getId(), CreditType.TODAY_CREDIT, referenceId);

        AttendanceStreak streak = getOrCreateStreak(user, today);
        boolean streakBonusEarned = updateStreak(streak, today, user.getId());

        int streakBonusPoints = streakBonusEarned ? STREAK_REWARD_POINTS : 0;
        int totalPoints = creditService.getTotalPoints(user.getId());

        return new CheckResponse(
                user.getUserTag(),
                LocalDateTime.now(SEOUL_ZONE),
                dailyPoints,
                streakBonusEarned,
                streakBonusPoints,
                streak.getCurrentStreak(),
                totalPoints
        );
    }

    public WeeklyResponse getWeekly() {
        User user = userService.findCurrentUser();
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<AttendanceRecord> records =
                attendanceRecordRepository.findByUserIdAndAttendedDateBetween(user.getId(), monday, sunday);

        Set<LocalDate> attendedDates = records.stream()
                .map(AttendanceRecord::getAttendedDate)
                .collect(Collectors.toSet());

        String[] dayLabels = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        List<DayAttendance> weeklyAttendance = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            AttendanceStatus status;
            int points = 0;

            if (attendedDates.contains(date)) {
                status = AttendanceStatus.ATTENDED;
                points = CreditType.TODAY_CREDIT.getDefaultAmount();
            } else if (date.equals(today)) {
                status = AttendanceStatus.PENDING;
            } else {
                status = AttendanceStatus.NOT_ATTENDED;
            }

            weeklyAttendance.add(new DayAttendance(dayLabels[i], date, status, points));
        }

        AttendanceStreak streak = attendanceStreakRepository.findByUserId(user.getId()).orElse(null);
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        boolean isStreakAchieved = streak != null && streak.isStreakAchieved();

        return new WeeklyResponse(
                user.getUserTag(),
                monday,
                currentStreak,
                isStreakAchieved,
                weeklyAttendance,
                STREAK_REWARD_POINTS
        );
    }

    public SummaryResponse getSummary() {
        User user = userService.findCurrentUser();

        long totalAttendedDays = attendanceRecordRepository.countByUserId(user.getId());

        AttendanceStreak streak = attendanceStreakRepository.findByUserId(user.getId()).orElse(null);
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int maxStreak = streak != null ? streak.getMaxStreak() : 0;

        int totalPoints = creditService.getTotalPoints(user.getId());

        LocalDateTime lastAttendedAt = attendanceRecordRepository
                .findTopByUserIdOrderByAttendedDateDesc(user.getId())
                .map(r -> r.getAttendedDate().atStartOfDay())
                .orElse(null);

        return new SummaryResponse(
                user.getUserTag(),
                totalAttendedDays,
                currentStreak,
                maxStreak,
                totalPoints,
                lastAttendedAt
        );
    }

    private AttendanceStreak getOrCreateStreak(User user, LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return attendanceStreakRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    // detached User 대신 같은 영속성 컨텍스트에서 다시 조회
                    User managedUser = userRepository.findById(user.getId())
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                    return attendanceStreakRepository.save(
                            AttendanceStreak.builder()
                                    .user(managedUser)
                                    .currentStreak(0)
                                    .maxStreak(0)
                                    .streakWeekStart(weekStart)
                                    .isStreakAchieved(false)
                                    .build()
                    );
                });
    }

    private boolean updateStreak(AttendanceStreak streak, LocalDate today, Long userId) {
        LocalDate yesterday = today.minusDays(1);
        boolean continuedStreak = attendanceRecordRepository
                .existsByUserIdAndAttendedDate(userId, yesterday);

        if (continuedStreak || streak.getCurrentStreak() == 0) {
            streak.attend(today);
        } else {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            streak.resetStreak(weekStart);
        }

        boolean justAchieved = streak.getCurrentStreak() >= STREAK_BONUS_THRESHOLD
                && streak.isStreakAchieved()
                && !creditService.existsHistory(userId, CreditType.ATTENDANCE_STREAK, toDateLong(streak.getStreakWeekStart()) + 7L);

        if (justAchieved) {
            Long streakReferenceId = toDateLong(streak.getStreakWeekStart()) + 7L;
            creditService.addCredit(userId, CreditType.ATTENDANCE_STREAK, streakReferenceId);
        }

        return justAchieved;
    }

    private Long toDateLong(LocalDate date) {
        return Long.parseLong(date.toString().replace("-", ""));
    }
}
