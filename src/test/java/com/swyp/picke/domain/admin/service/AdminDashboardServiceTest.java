package com.swyp.picke.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardSummaryResponse;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserDailyActivityRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.time.LocalDate;
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
}
