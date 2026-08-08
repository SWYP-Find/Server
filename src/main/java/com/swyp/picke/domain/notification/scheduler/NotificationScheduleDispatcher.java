package com.swyp.picke.domain.notification.scheduler;

import com.swyp.picke.domain.notification.entity.NotificationSchedule;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.repository.NotificationScheduleRepository;
import com.swyp.picke.domain.notification.service.NotificationDispatchService;
import com.swyp.picke.domain.notification.service.NotificationService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매분 활성화된 예약 알림을 조회해 현재 시각(분 단위)과 일치하면 전체 사용자에게 푸시를 발송한다.
 * 같은 예약이 하루에 두 번 발송되지 않도록 {@link NotificationSchedule#markSent}로 발송일을 기록한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduleDispatcher {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationService notificationService;
    private final NotificationDispatchService notificationDispatchService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @Transactional
    public void dispatchDueSchedules() {
        LocalTime now = LocalTime.now(SEOUL_ZONE);
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        List<NotificationSchedule> dueSchedules = notificationScheduleRepository.findAllByEnabledTrue().stream()
                .filter(schedule -> schedule.isDue(now, today))
                .toList();

        for (NotificationSchedule schedule : dueSchedules) {
            notificationService.createBroadcastNotification(
                    NotificationDetailCode.DAILY_MESSAGE, schedule.getTitle(), schedule.getSubtitle(), null);
            notificationDispatchService.notifyAdminNotice(
                    NotificationDetailCode.DAILY_MESSAGE, schedule.getTitle(), schedule.getSubtitle());
            schedule.markSent(today);
            log.info("[NotificationScheduleDispatcher] sent scheduleId={}, title={}", schedule.getId(), schedule.getTitle());
        }
    }
}
