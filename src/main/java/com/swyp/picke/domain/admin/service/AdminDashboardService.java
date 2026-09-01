package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardSummaryResponse;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserDailyActivityRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
}
