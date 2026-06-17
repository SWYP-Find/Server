package com.swyp.picke.global.infra.apns.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.swyp.picke.domain.notification.entity.UserDevice;
import com.swyp.picke.domain.notification.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * iOS 디바이스에 FCM을 거치지 않고 APNs로 직접 푸시를 발송한다.
 * apns.enabled=false(미설정)인 환경에서는 ApnsClient 빈이 없으므로 발송을 건너뛴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApnsPushService {

    private static final Set<String> INVALID_TOKEN_REASONS = Set.of(
            "BadDeviceToken", "Unregistered", "DeviceTokenNotForTopic");

    private final Optional<ApnsClient> apnsClient;
    private final UserDeviceRepository userDeviceRepository;

    @Value("${apns.bundle-id}")
    private String bundleId;

    public void send(UserDevice device, String title, String body, Map<String, String> data) {
        if (apnsClient.isEmpty()) {
            log.warn("APNs가 설정되지 않아 푸시를 건너뜁니다. deviceId={}", device.getId());
            return;
        }

        ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder()
                .setAlertTitle(title)
                .setAlertBody(body)
                .setSound("default");
        data.forEach(payloadBuilder::addCustomProperty);

        String token = TokenUtil.sanitizeTokenString(device.getFcmToken());
        SimpleApnsPushNotification notification =
                new SimpleApnsPushNotification(token, bundleId, payloadBuilder.build());

        apnsClient.get().sendNotification(notification).whenComplete((response, cause) -> {
            if (cause != null) {
                log.warn("APNs 푸시 전송 실패. deviceId={}, error={}", device.getId(), cause.getMessage());
                return;
            }
            handleResponse(device, response);
        });
    }

    private void handleResponse(UserDevice device, PushNotificationResponse<SimpleApnsPushNotification> response) {
        if (response.isAccepted()) {
            return;
        }

        String reason = response.getRejectionReason().orElse("UNKNOWN");
        log.warn("APNs 푸시 거부됨. deviceId={}, reason={}", device.getId(), reason);

        if (INVALID_TOKEN_REASONS.contains(reason)) {
            userDeviceRepository.deleteById(device.getId());
        }
    }
}
