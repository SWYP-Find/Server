package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardBattleStatsResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardDauMauResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardNewUsersResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardSummaryResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardTrendItemResponse;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.battle.repository.projection.BattleParticipationStats;
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
    private final BattleRepository battleRepository;

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

    public AdminDashboardNewUsersResponse getNewUsersTrend(LocalDate from, LocalDate to, String granularity) {
        if (from.isAfter(to)) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }

        List<DailyUserCount> rows = "week".equalsIgnoreCase(granularity)
                ? userRepository.findWeeklyNewUserCounts(from, to)
                : userRepository.findDailyNewUserCounts(from, to);

        List<AdminDashboardTrendItemResponse> items = rows.stream()
                .map(row -> new AdminDashboardTrendItemResponse(row.getActivityDate(), row.getCount()))
                .toList();

        // week 단위 items는 주 경계가 from~to 범위를 벗어날 수 있어(예: to가 주 중간이면 그 주 전체를 포함),
        // 요청한 기간 전체의 정확한 합계는 items 합산이 아니라 별도 카운트로 계산한다.
        long totalCount = userRepository.countByCreatedAtBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        return new AdminDashboardNewUsersResponse(totalCount, items);
    }

    public AdminDashboardBattleStatsResponse getBattleStats(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }

        long totalUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        BattleParticipationStats stats = battleRepository.findParticipationStats(from, to, totalUsers);

        return new AdminDashboardBattleStatsResponse(
                stats.getBattleCount(),
                orZero(stats.getAvgPreVoteRate()),
                orZero(stats.getAvgPostVoteRate()),
                orZero(stats.getAvgPerspectiveRate()),
                orZero(stats.getAvgCommentRate())
        );
    }

    private double orZero(Double value) {
        return value != null ? value : 0.0;
    }
}
