package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardDauMauResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardSummaryResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardTrendItemResponse;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserDailyActivityRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.repository.projection.DailyUserCount;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final UserDailyActivityRepository userDailyActivityRepository;

    public AdminDashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        long newUserCount = userRepository.countByCreatedAtBetween(todayStart, tomorrowStart);
        long loginUserCount = userDailyActivityRepository.countByActivityDateAndLoggedInTrue(today);
        long activeUserCount = userDailyActivityRepository.countByActivityDateAndActiveTrue(today);
        long totalUserCount = userRepository.countByStatus(UserStatus.ACTIVE);

        return new AdminDashboardSummaryResponse(newUserCount, loginUserCount, activeUserCount, totalUserCount);
    }

    public AdminDashboardDauMauResponse getDauMauTrend(LocalDate from, LocalDate to, String granularity) {
        if (from.isAfter(to)) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }

        List<DailyUserCount> rows = "month".equalsIgnoreCase(granularity)
                ? userDailyActivityRepository.findRollingMonthlyActiveUserCounts(from, to)
                : userDailyActivityRepository.findDailyActiveUserCounts(from, to);

        List<AdminDashboardTrendItemResponse> items = rows.stream()
                .map(row -> new AdminDashboardTrendItemResponse(row.getActivityDate(), row.getCount()))
                .toList();

        return new AdminDashboardDauMauResponse(items);
    }
}
