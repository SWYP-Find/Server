package com.swyp.picke.domain.notification.scheduler;

import com.swyp.picke.domain.notification.entity.Notification;
import com.swyp.picke.domain.notification.entity.NotificationSchedule;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.repository.NotificationScheduleRepository;
import com.swyp.picke.domain.notification.service.NotificationDispatchService;
import com.swyp.picke.domain.notification.service.NotificationService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
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

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationService notificationService;
    private final NotificationDispatchService notificationDispatchService;
    private final Clock clock;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @Transactional
    public void dispatchDueSchedules() {
        LocalTime now = LocalTime.now(clock);
        LocalDate today = LocalDate.now(clock);

        List<NotificationSchedule> dueSchedules = notificationScheduleRepository.findAllByEnabledTrue().stream()
                .filter(schedule -> schedule.isDue(now, today))
                .toList();

        for (NotificationSchedule schedule : dueSchedules) {
            Notification notification = notificationService.createBroadcastNotification(
                    NotificationDetailCode.DAILY_MESSAGE, schedule.getTitle(), schedule.getSubtitle(), null);
            notificationDispatchService.notifyAdminNotice(
                    notification.getId(), NotificationDetailCode.DAILY_MESSAGE, schedule.getTitle(), schedule.getSubtitle());
            schedule.markSent(today);
            log.info("[NotificationScheduleDispatcher] sent scheduleId={}, title={}", schedule.getId(), schedule.getTitle());
        }
    }
}
