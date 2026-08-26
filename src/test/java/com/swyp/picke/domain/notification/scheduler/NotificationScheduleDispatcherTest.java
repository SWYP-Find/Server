package com.swyp.picke.domain.notification.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.notification.entity.Notification;
import com.swyp.picke.domain.notification.entity.NotificationSchedule;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.repository.NotificationScheduleRepository;
import com.swyp.picke.domain.notification.service.NotificationDispatchService;
import com.swyp.picke.domain.notification.service.NotificationService;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationScheduleDispatcherTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private NotificationScheduleRepository notificationScheduleRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    private NotificationScheduleDispatcher notificationScheduleDispatcher;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                LocalTime.of(9, 0).atDate(java.time.LocalDate.now(SEOUL_ZONE)).atZone(SEOUL_ZONE).toInstant(),
                SEOUL_ZONE);
        notificationScheduleDispatcher = new NotificationScheduleDispatcher(
                notificationScheduleRepository, notificationService, notificationDispatchService, fixedClock);
    }

    @Test
    @DisplayName("현재 시각과 일치하는 활성 예약만 발송하고 발송일을 기록한다")
    void dispatchDueSchedules_sendsOnlyMatchingEnabledSchedules() {
        LocalTime now = LocalTime.of(9, 0);
        NotificationSchedule due = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(now.getHour(), now.getMinute()))
                .enabled(true)
                .build();
        NotificationSchedule notDue = NotificationSchedule.builder()
                .title("다른 알림")
                .subtitle("다른 소제목")
                .sendTime(LocalTime.of(now.getHour(), now.getMinute()).plusHours(1))
                .enabled(true)
                .build();
        when(notificationScheduleRepository.findAllByEnabledTrue()).thenReturn(List.of(due, notDue));

        Notification notification = Notification.builder()
                .user(null)
                .category(NotificationCategory.CONTENT)
                .detailCode(NotificationDetailCode.DAILY_MESSAGE)
                .title("오늘의 질문")
                .body("지금 확인해보세요")
                .build();
        ReflectionTestUtils.setField(notification, "id", 1L);
        when(notificationService.createBroadcastNotification(
                eq(NotificationDetailCode.DAILY_MESSAGE), eq("오늘의 질문"), eq("지금 확인해보세요"), any()))
                .thenReturn(notification);

        notificationScheduleDispatcher.dispatchDueSchedules();

        verify(notificationService, times(1)).createBroadcastNotification(
                eq(NotificationDetailCode.DAILY_MESSAGE), eq("오늘의 질문"), eq("지금 확인해보세요"), any());
        verify(notificationDispatchService, times(1)).notifyAdminNotice(
                eq(1L), eq(NotificationDetailCode.DAILY_MESSAGE), eq("오늘의 질문"), eq("지금 확인해보세요"));
        verify(notificationDispatchService, never()).notifyAdminNotice(
                any(), eq(NotificationDetailCode.DAILY_MESSAGE), eq("다른 알림"), any());
    }

    @Test
    @DisplayName("발송 대상 예약이 없으면 아무것도 발송하지 않는다")
    void dispatchDueSchedules_doesNothing_whenNoScheduleIsDue() {
        when(notificationScheduleRepository.findAllByEnabledTrue()).thenReturn(List.of());

        notificationScheduleDispatcher.dispatchDueSchedules();

        verify(notificationDispatchService, never()).notifyAdminNotice(any(), any(), any(), any());
    }
}
