package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.attendance.response.AdminAttendanceResponse.StatsResponse;
import com.swyp.picke.domain.admin.dto.attendance.response.AdminAttendanceResponse.StatsResponse.DailyStat;
import com.swyp.picke.domain.admin.dto.attendance.response.AdminAttendanceResponse.UserAttendanceResponse;
import com.swyp.picke.domain.admin.dto.attendance.response.AdminAttendanceResponse.UserAttendanceResponse.AttendanceItem;
import com.swyp.picke.domain.admin.dto.attendance.response.AdminAttendanceResponse.UserAttendanceResponse.AttendanceSummary;
import com.swyp.picke.domain.attendance.entity.AttendanceRecord;
import com.swyp.picke.domain.attendance.entity.AttendanceStreak;
import com.swyp.picke.domain.attendance.repository.AttendanceRecordRepository;
import com.swyp.picke.domain.attendance.repository.AttendanceStreakRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAttendanceService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int MAX_DATE_RANGE_DAYS = 90;

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceStreakRepository attendanceStreakRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CreditService creditService;

    public StatsResponse getStats(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        if (from == null) from = today.minusDays(6);
        if (to == null) to = today;

        if (from.isAfter(to) || to.minusDays(MAX_DATE_RANGE_DAYS).isAfter(from)) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }

        long totalUsers = userRepository.count();

        List<Object[]> rawStats = attendanceRecordRepository.countAttendanceGroupByDate(from, to);
        Map<LocalDate, Long> countMap = new HashMap<>();
        for (Object[] row : rawStats) {
            countMap.put((LocalDate) row[0], (Long) row[1]);
        }

        List<DailyStat> dailyStats = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            long attendedCount = countMap.getOrDefault(cursor, 0L);
            double rate = totalUsers > 0
                    ? Math.round((attendedCount * 1000.0 / totalUsers)) / 10.0
                    : 0.0;
            dailyStats.add(new DailyStat(cursor, attendedCount, rate));
            cursor = cursor.plusDays(1);
        }

        return new StatsResponse(from, to, totalUsers, dailyStats);
    }

    public UserAttendanceResponse getUserAttendance(String userTag, int page, int size) {
        User user = userService.findByUserTag(userTag);

        long totalAttendedDays = attendanceRecordRepository.countByUserId(user.getId());

        AttendanceStreak streak = attendanceStreakRepository.findByUserId(user.getId()).orElse(null);
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int maxStreak = streak != null ? streak.getMaxStreak() : 0;

        int totalPoints = creditService.getTotalPoints(user.getId());

        LocalDateTime lastAttendedAt = attendanceRecordRepository
                .findTopByUserIdOrderByAttendedDateDesc(user.getId())
                .map(r -> r.getAttendedDate().atStartOfDay())
                .orElse(null);

        AttendanceSummary summary = new AttendanceSummary(
                totalAttendedDays,
                currentStreak,
                maxStreak,
                totalPoints,
                lastAttendedAt
        );

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "attendedDate"));
        Page<AttendanceRecord> recordPage = attendanceRecordRepository.findByUserId(user.getId(), pageable);

        List<AttendanceItem> items = recordPage.getContent().stream()
                .map(r -> new AttendanceItem(
                        r.getAttendedDate().atStartOfDay(),
                        0,
                        r.getPointsEarned(),
                        false
                ))
                .toList();

        return new UserAttendanceResponse(
                user.getUserTag(),
                summary,
                recordPage.getTotalElements(),
                page,
                size,
                items
        );
    }
}
