package com.swyp.picke.domain.user.service.batch;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.CreditService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주간 자동 충전 배치.
 * 활성 사용자(UserStatus.ACTIVE) 전원에게 +40P (CreditType.WEEKLY_CHARGE) 지급.
 *
 * referenceId = 배치 실행 월요일의 yyyyMMdd 정수. 같은 주차 재실행 시 중복 지급 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyChargeJob {

    private static final DateTimeFormatter KEY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final UserRepository userRepository;
    private final CreditService creditService;

    @Transactional
    public void run(LocalDate runDate) {
        Long weekKey = Long.parseLong(runDate.format(KEY_FORMAT));
        List<User> activeUsers = userRepository.findAllByStatus(UserStatus.ACTIVE);

        log.info("[WeeklyChargeJob] runDate={} activeUsers={}", runDate, activeUsers.size());

        for (User user : activeUsers) {
            creditService.addCredit(user.getId(), CreditType.WEEKLY_CHARGE, weekKey);
        }
    }
}
