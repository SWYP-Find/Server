package com.swyp.picke.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.notification.entity.NotificationDeliveryResult;
import com.swyp.picke.domain.notification.entity.UserDevice;
import com.swyp.picke.domain.notification.enums.DevicePlatform;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.repository.NotificationDeliveryResultRepository;
import com.swyp.picke.domain.notification.repository.UserDeviceRepository;
import com.swyp.picke.domain.user.repository.UserSettingsRepository;
import com.swyp.picke.global.infra.apns.service.ApnsPushService;
import com.swyp.picke.global.infra.fcm.service.FcmPushService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private NotificationDeliveryResultRepository notificationDeliveryResultRepository;

    @Mock
    private FcmPushService fcmPushService;

    @Mock
    private ApnsPushService apnsPushService;

    private NotificationDispatchService newService() {
        NotificationDispatchService service = new NotificationDispatchService(
                notificationService, userDeviceRepository, userSettingsRepository,
                notificationDeliveryResultRepository, fcmPushService, apnsPushService);
        ReflectionTestUtils.setField(service, "baseUrl", "https://picke.store");
        return service;
    }

    private UserDevice newDevice() {
        return UserDevice.builder().fcmToken("token-" + Math.random()).platform(DevicePlatform.ANDROID).build();
    }

    @Test
    @DisplayName("공지 발송 시 대상 디바이스 수로 발송 결과 row를 먼저 만들고, 발송 완료 후 성공/실패 건수를 갱신한다")
    void notifyAdminNotice_recordsDeliveryResult() {
        NotificationDispatchService notificationDispatchService = newService();

        UserDevice success = newDevice();
        UserDevice failure = newDevice();
        when(userSettingsRepository.findUserIdsByMarketingEventEnabledTrue()).thenReturn(List.of(1L, 2L));
        when(userDeviceRepository.findAllByUserIdIn(List.of(1L, 2L))).thenReturn(List.of(success, failure));
        when(fcmPushService.send(eq(success), anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(fcmPushService.send(eq(failure), anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(false));

        notificationDispatchService.notifyAdminNotice(10L, NotificationDetailCode.POLICY_CHANGE, "제목", "본문");

        ArgumentCaptor<NotificationDeliveryResult> createdCaptor = ArgumentCaptor.forClass(NotificationDeliveryResult.class);
        verify(notificationDeliveryResultRepository).save(createdCaptor.capture());
        assertThat(createdCaptor.getValue().getNotificationId()).isEqualTo(10L);
        assertThat(createdCaptor.getValue().getTargetCount()).isEqualTo(2);

        verify(notificationDeliveryResultRepository).updateResult(10L, 1, 1);
    }

    @Test
    @DisplayName("공지 발송 대상자 수를 마케팅/이벤트 알림 설정 ON인 유저의 디바이스 수 기준으로 조회한다")
    void countAdminNoticeTargets_returnsDeviceCount() {
        NotificationDispatchService notificationDispatchService = newService();

        when(userSettingsRepository.findUserIdsByMarketingEventEnabledTrue()).thenReturn(List.of(1L, 2L, 3L));
        when(userDeviceRepository.countByUserIdIn(List.of(1L, 2L, 3L))).thenReturn(5L);

        int targetCount = notificationDispatchService.countAdminNoticeTargets();

        assertThat(targetCount).isEqualTo(5);
    }
}
