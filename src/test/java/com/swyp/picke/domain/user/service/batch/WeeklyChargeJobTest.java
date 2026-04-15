package com.swyp.picke.domain.user.service.batch;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.CreditService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyChargeJobTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private WeeklyChargeJob job;

    @Test
    @DisplayName("활성 사용자에게만 WEEKLY_CHARGE 를 지급한다")
    void run_rewardsOnlyActiveUsers() {
        User activeUser1 = user(1L);
        User activeUser2 = user(2L);
        LocalDate runDate = LocalDate.of(2026, 4, 13);

        when(userRepository.findAllByStatus(UserStatus.ACTIVE)).thenReturn(List.of(activeUser1, activeUser2));

        job.run(runDate);

        verify(creditService).addCredit(1L, CreditType.WEEKLY_CHARGE, 20260413L);
        verify(creditService).addCredit(2L, CreditType.WEEKLY_CHARGE, 20260413L);
    }

    private User user(Long id) {
        User user = User.builder()
                .userTag("user-" + id)
                .nickname("nick")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
