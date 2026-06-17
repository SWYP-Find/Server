package com.swyp.picke.global.infra.fcm.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.swyp.picke.domain.notification.entity.UserDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService {

    private final FirebaseMessaging firebaseMessaging;

    /**
     * Android 디바이스에 FCM data-only 메시지를 발송한다.
     * iOS는 FCM을 거치지 않고 ApnsPushService가 APNs로 직접 발송한다.
     */
    public void send(UserDevice device, String title, String body, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(device.getFcmToken())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 푸시 전송 실패. deviceId={}, error={}", device.getId(), e.getMessage());
        }
    }
}
