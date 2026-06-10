package com.swyp.picke.global.infra.fcm.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.swyp.picke.domain.notification.entity.UserDevice;
import com.swyp.picke.domain.notification.enums.DevicePlatform;
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
     * 디바이스 플랫폼에 맞는 FCM 메시지를 구성해 발송한다.
     * Android는 data-only 페이로드로, iOS는 notification + data(url 포함)로 분기한다.
     */
    public void send(UserDevice device, String title, String body, Map<String, String> data) {
        Message message = device.getPlatform() == DevicePlatform.IOS
                ? buildIosMessage(device.getFcmToken(), title, body, data)
                : buildAndroidMessage(device.getFcmToken(), data);

        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 푸시 전송 실패. token={}, platform={}, error={}",
                    device.getFcmToken(), device.getPlatform(), e.getMessage());
        }
    }

    private Message buildAndroidMessage(String token, Map<String, String> data) {
        return Message.builder()
                .setToken(token)
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();
    }

    private Message buildIosMessage(String token, String title, String body, Map<String, String> data) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .build())
                        .build())
                .build();
    }
}
