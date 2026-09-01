package com.swyp.picke.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardDauMauResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardSummaryResponse;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserDailyActivityRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.repository.projection.DailyUserCount;
import com.swyp.picke.global.common.exception.CustomException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDailyActivityRepository userDailyActivityRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("오늘 요약 카드를 조회한다")
    void getSummary_returnsTodaySummary() {
        when(userRepository.countByCreatedAtBetween(any(), any())).thenReturn(12L);
        when(userDailyActivityRepository.countByActivityDateAndLoggedInTrue(eq(LocalDate.now()))).thenReturn(340L);
        when(userDailyActivityRepository.countByActivityDateAndActiveTrue(eq(LocalDate.now()))).thenReturn(500L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(58000L);

        AdminDashboardSummaryResponse response = adminDashboardService.getSummary();

        assertThat(response.newUserCount()).isEqualTo(12L);
        assertThat(response.loginUserCount()).isEqualTo(340L);
        assertThat(response.activeUserCount()).isEqualTo(500L);
        assertThat(response.totalUserCount()).isEqualTo(58000L);
    }

    @Test
    @DisplayName("granularity=day면 일자별 DAU 카운트를 조회한다")
    void getDauMauTrend_day_returnsDailyActiveCounts() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 2);
        List<DailyUserCount> rows = List.of(
                dailyUserCount(LocalDate.of(2026, 8, 1), 10L),
                dailyUserCount(LocalDate.of(2026, 8, 2), 15L)
        );
        when(userDailyActivityRepository.findDailyActiveUserCounts(from, to)).thenReturn(rows);

        AdminDashboardDauMauResponse response = adminDashboardService.getDauMauTrend(from, to, "day");

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).date()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.items().get(0).count()).isEqualTo(10L);
        assertThat(response.items().get(1).count()).isEqualTo(15L);
    }

    @Test
    @DisplayName("granularity=month면 롤링 30일 MAU 카운트를 조회한다")
    void getDauMauTrend_month_returnsRollingMonthlyCounts() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 1);
        List<DailyUserCount> rows = List.of(dailyUserCount(LocalDate.of(2026, 8, 1), 1200L));
        when(userDailyActivityRepository.findRollingMonthlyActiveUserCounts(from, to)).thenReturn(rows);

        AdminDashboardDauMauResponse response = adminDashboardService.getDauMauTrend(from, to, "month");

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).count()).isEqualTo(1200L);
    }

    @Test
    @DisplayName("from이 to보다 늦으면 예외를 던진다")
    void getDauMauTrend_throws_whenFromIsAfterTo() {
        LocalDate from = LocalDate.of(2026, 8, 10);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> adminDashboardService.getDauMauTrend(from, to, "day"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("granularity=day면 일자별 신규 가입자 수를 조회한다")
    void getNewUsersTrend_day_returnsDailyCounts() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 2);
        List<DailyUserCount> rows = List.of(
                dailyUserCount(LocalDate.of(2026, 8, 1), 3L),
                dailyUserCount(LocalDate.of(2026, 8, 2), 5L)
        );
        when(userRepository.findDailyNewUserCounts(from, to)).thenReturn(rows);
        when(userRepository.countByCreatedAtBetween(any(), any())).thenReturn(8L);

        var response = adminDashboardService.getNewUsersTrend(from, to, "day");

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(1).count()).isEqualTo(5L);
        assertThat(response.totalCount()).isEqualTo(8L);
    }

    @Test
    @DisplayName("granularity=week면 주별 신규 가입자 수를 조회한다")
    void getNewUsersTrend_week_returnsWeeklyCounts() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 14);
        List<DailyUserCount> rows = List.of(dailyUserCount(LocalDate.of(2026, 7, 27), 20L));
        when(userRepository.findWeeklyNewUserCounts(from, to)).thenReturn(rows);
        when(userRepository.countByCreatedAtBetween(any(), any())).thenReturn(18L);

        var response = adminDashboardService.getNewUsersTrend(from, to, "week");

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).count()).isEqualTo(20L);
        assertThat(response.totalCount()).isEqualTo(18L);
    }

    @Test
    @DisplayName("신규 가입자 추이 조회 시 from이 to보다 늦으면 예외를 던진다")
    void getNewUsersTrend_throws_whenFromIsAfterTo() {
        LocalDate from = LocalDate.of(2026, 8, 10);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> adminDashboardService.getNewUsersTrend(from, to, "day"))
                .isInstanceOf(CustomException.class);
    }

    private DailyUserCount dailyUserCount(LocalDate date, long count) {
        return new DailyUserCount() {
            @Override
            public LocalDate getActivityDate() {
                return date;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }
}
