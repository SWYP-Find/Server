package com.swyp.picke.domain.notification.service;

import com.swyp.picke.domain.notification.entity.UserDevice;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.repository.UserDeviceRepository;
import com.swyp.picke.global.infra.fcm.service.FcmPushService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 앱 내 알림(Notification)과 FCM 푸시를 함께 발송하는 알림 정책별 디스패치 서비스.
 */
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationService notificationService;
    private final UserDeviceRepository userDeviceRepository;
    private final FcmPushService fcmPushService;

    @Value("${picke.baseUrl}")
    private String baseUrl;

    /**
     * 새로운 배틀 발행 시 전체 사용자에게 인앱 broadcast 알림 + FCM 푸시를 발송한다.
     */
    public void notifyNewBattle(Long battleId, String battleTitle) {
        String body = "\"" + battleTitle + "\"에 지금 참여해보세요!";

        notificationService.createBroadcastNotification(NotificationDetailCode.NEW_BATTLE, body, battleId);

        Map<String, String> data = Map.of(
                "type", "BATTLE",
                "battleId", String.valueOf(battleId),
                "url", baseUrl + "/battle/" + battleId
        );

        String pushTitle = NotificationDetailCode.NEW_BATTLE.getDefaultTitle();
        for (UserDevice device : userDeviceRepository.findAll()) {
            fcmPushService.send(device, pushTitle, body, data);
        }
    }
}
