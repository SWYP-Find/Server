package com.swyp.picke.global.infra.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.swyp.picke.domain.notification.entity.UserDevice;
import com.swyp.picke.domain.notification.enums.DevicePlatform;
import com.swyp.picke.domain.notification.repository.UserDeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Test
    void send_includes_title_and_body_in_data_payload() throws Exception {
        FcmPushService fcmPushService = new FcmPushService(firebaseMessaging, userDeviceRepository);
        UserDevice device = buildDevice();

        fcmPushService.send(device, "제목", "본문", Map.of("type", "TEST"));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging).send(messageCaptor.capture());

        Map<String, String> data = extractData(messageCaptor.getValue());
        assertThat(data).containsEntry("title", "제목");
        assertThat(data).containsEntry("body", "본문");
        assertThat(data).containsEntry("type", "TEST");
    }

    @Test
    void send_deletes_device_when_token_is_unregistered() throws Exception {
        FcmPushService fcmPushService = new FcmPushService(firebaseMessaging, userDeviceRepository);
        UserDevice device = buildDevice();

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        lenient().when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        doThrow(exception).when(firebaseMessaging).send(any(Message.class));

        fcmPushService.send(device, "제목", "본문", Map.of("type", "TEST"));

        verify(userDeviceRepository).deleteById(device.getId());
    }

    @Test
    void send_keeps_device_when_error_is_not_unregistered() throws Exception {
        FcmPushService fcmPushService = new FcmPushService(firebaseMessaging, userDeviceRepository);
        UserDevice device = buildDevice();

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        lenient().when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        doThrow(exception).when(firebaseMessaging).send(any(Message.class));

        fcmPushService.send(device, "제목", "본문", Map.of("type", "TEST"));

        verify(userDeviceRepository, never()).deleteById(any());
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
