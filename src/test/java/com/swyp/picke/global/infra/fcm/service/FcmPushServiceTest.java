package com.swyp.picke.global.infra.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.swyp.picke.domain.notification.entity.UserDevice;
import com.swyp.picke.domain.notification.enums.DevicePlatform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Test
    void send_includes_title_and_body_in_data_payload() throws Exception {
        FcmPushService fcmPushService = new FcmPushService(firebaseMessaging);
        UserDevice device = buildDevice();

        fcmPushService.send(device, "제목", "본문", Map.of("type", "TEST"));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging).send(messageCaptor.capture());

        Map<String, String> data = extractData(messageCaptor.getValue());
        assertThat(data).containsEntry("title", "제목");
        assertThat(data).containsEntry("body", "본문");
        assertThat(data).containsEntry("type", "TEST");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractData(Message message) throws Exception {
        Field dataField = Message.class.getDeclaredField("data");
        dataField.setAccessible(true);
        return (Map<String, String>) dataField.get(message);
    }

    private UserDevice buildDevice() throws Exception {
        UserDevice device = UserDevice.builder()
                .fcmToken("token")
                .platform(DevicePlatform.ANDROID)
                .build();
        Field idField = device.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(device, 1L);
        return device;
    }
}
