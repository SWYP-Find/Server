package com.swyp.picke.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardAttendanceStatsResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardBattleStatsResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardDauMauResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardSummaryResponse;
import com.swyp.picke.domain.attendance.repository.AttendanceRecordRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.battle.repository.projection.BattleParticipationStats;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.CreditHistoryRepository;
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

    @Mock
    private BattleRepository battleRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private CreditHistoryRepository creditHistoryRepository;

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

    @Test
    @DisplayName("배틀당 평균 참여율을 조회한다")
    void getBattleStats_returnsAverageRates() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 7);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(1000L);
        BattleParticipationStats stats = battleParticipationStats(5L, 0.8, 0.6, 0.4, 0.1);
        when(battleRepository.findParticipationStats(from, to, 1000L)).thenReturn(stats);

        AdminDashboardBattleStatsResponse response = adminDashboardService.getBattleStats(from, to);

        assertThat(response.battleCount()).isEqualTo(5L);
        assertThat(response.avgPreVoteRate()).isEqualTo(0.8);
        assertThat(response.avgPostVoteRate()).isEqualTo(0.6);
        assertThat(response.avgPerspectiveWriteRate()).isEqualTo(0.4);
        assertThat(response.avgCommentWriteRate()).isEqualTo(0.1);
    }

    @Test
    @DisplayName("기간 내 발행된 배틀이 없으면 참여율은 0으로 반환한다")
    void getBattleStats_returnsZero_whenNoBattlesPublished() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 7);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(1000L);
        BattleParticipationStats stats = battleParticipationStats(0L, null, null, null, null);
        when(battleRepository.findParticipationStats(from, to, 1000L)).thenReturn(stats);

        AdminDashboardBattleStatsResponse response = adminDashboardService.getBattleStats(from, to);

        assertThat(response.battleCount()).isEqualTo(0L);
        assertThat(response.avgPreVoteRate()).isEqualTo(0.0);
        assertThat(response.avgCommentWriteRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("배틀 참여율 조회 시 from이 to보다 늦으면 예외를 던진다")
    void getBattleStats_throws_whenFromIsAfterTo() {
        LocalDate from = LocalDate.of(2026, 8, 10);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> adminDashboardService.getBattleStats(from, to))
                .isInstanceOf(CustomException.class);
    }

    private BattleParticipationStats battleParticipationStats(
            long battleCount, Double preVote, Double postVote, Double perspective, Double comment) {
        return new BattleParticipationStats() {
            @Override
            public long getBattleCount() {
                return battleCount;
            }

            @Override
            public Double getAvgPreVoteRate() {
                return preVote;
            }

            @Override
            public Double getAvgPostVoteRate() {
                return postVote;
            }

            @Override
            public Double getAvgPerspectiveRate() {
                return perspective;
            }

            @Override
            public Double getAvgCommentRate() {
                return comment;
            }
        };
    }

    @Test
    @DisplayName("출석 체크율과 개근 보너스 달성 건수를 조회한다")
    void getAttendanceStats_returnsRateAndStreakCount() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 2);
        List<DailyUserCount> rows = List.of(
                dailyUserCount(LocalDate.of(2026, 8, 1), 100L),
                dailyUserCount(LocalDate.of(2026, 8, 2), 200L)
        );
        when(attendanceRecordRepository.findDailyAttendanceCounts(from, to)).thenReturn(rows);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(1000L);
        when(creditHistoryRepository.countByCreditTypeAndCreatedAtBetween(
                eq(CreditType.ATTENDANCE_STREAK), any(), any())).thenReturn(42L);

        AdminDashboardAttendanceStatsResponse response = adminDashboardService.getAttendanceStats(from, to);

        assertThat(response.items()).hasSize(2);
        assertThat(response.totalCount()).isEqualTo(300L);
        assertThat(response.avgAttendanceRate()).isCloseTo(0.15, org.assertj.core.data.Offset.offset(0.0001)); // (0.1 + 0.2) / 2
        assertThat(response.streakAchievedCount()).isEqualTo(42L);
    }

    @Test
    @DisplayName("출석 체크율 조회 시 from이 to보다 늦으면 예외를 던진다")
    void getAttendanceStats_throws_whenFromIsAfterTo() {
        LocalDate from = LocalDate.of(2026, 8, 10);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> adminDashboardService.getAttendanceStats(from, to))
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
