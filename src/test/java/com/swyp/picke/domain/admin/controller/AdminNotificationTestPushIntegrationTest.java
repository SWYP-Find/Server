package com.swyp.picke.domain.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.notification.entity.UserDevice;
import com.swyp.picke.domain.notification.enums.DevicePlatform;
import com.swyp.picke.domain.notification.repository.UserDeviceRepository;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.infra.apns.service.ApnsPushService;
import com.swyp.picke.global.infra.fcm.service.FcmPushService;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminNotificationTestPushIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3PresignedUrlService s3PresignedUrlService;

    @MockitoBean
    private FcmPushService fcmPushService;

    @MockitoBean
    private ApnsPushService apnsPushService;

    @Test
    @DisplayName("admin can send a test push to a specific user's registered device")
    void admin_can_send_test_push_to_user_device() throws Exception {
        String adminToken = createAdminToken();

        User targetUser = userRepository.save(
                User.builder()
                        .userTag("target-" + UUID.randomUUID().toString().substring(0, 8))
                        .nickname("target")
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()
        );

        UserDevice device = userDeviceRepository.save(
                UserDevice.builder()
                        .user(targetUser)
                        .fcmToken("test-token-" + UUID.randomUUID())
                        .platform(DevicePlatform.ANDROID)
                        .build()
        );

        Map<String, Object> payload = Map.of(
                "userId", targetUser.getId(),
                "title", "테스트 알림",
                "body", "테스트 발송 본문"
        );

        mockMvc.perform(post("/api/v1/admin/notices/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        ArgumentCaptor<UserDevice> deviceCaptor = ArgumentCaptor.forClass(UserDevice.class);
        verify(fcmPushService).send(deviceCaptor.capture(), eq("테스트 알림"), eq("테스트 발송 본문"), any());
        assertThat(deviceCaptor.getValue().getId()).isEqualTo(device.getId());
    }

    private String createAdminToken() {
        User admin = userRepository.save(
                User.builder()
                        .userTag("adm-" + UUID.randomUUID().toString().substring(0, 8))
                        .nickname("adm")
                        .role(UserRole.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build()
        );
        return jwtProvider.createAccessToken(admin.getId(), UserRole.ADMIN.name());
    }
}
