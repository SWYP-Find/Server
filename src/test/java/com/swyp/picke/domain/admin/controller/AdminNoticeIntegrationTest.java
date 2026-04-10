package com.swyp.picke.domain.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.notification.entity.Notification;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.repository.NotificationRepository;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminNoticeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3PresignedUrlService s3PresignedUrlService;

    @Test
    @DisplayName("관리자 공지 생성 및 목록 조회가 동작한다")
    void admin_can_create_and_list_notices() throws Exception {
        String adminToken = createAdminToken();

        Map<String, Object> payload = Map.of(
                "category", "NOTICE",
                "title", "서비스 점검 안내",
                "body", "오늘 22시에 점검이 진행됩니다.",
                "referenceId", 123L
        );

        mockMvc.perform(post("/api/v1/admin/notices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").exists())
                .andExpect(jsonPath("$.data.category").value("NOTICE"))
                .andExpect(jsonPath("$.data.title").value("서비스 점검 안내"));

        Notification saved = notificationRepository.findAll().stream()
                .filter(notification -> "서비스 점검 안내".equals(notification.getTitle()))
                .findFirst()
                .orElseThrow();

        assertThat(saved.getUser()).isNull();
        assertThat(saved.getCategory()).isEqualTo(NotificationCategory.NOTICE);

        mockMvc.perform(get("/api/v1/admin/notices")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].notificationId").exists())
                .andExpect(jsonPath("$.data.items[0].title").isNotEmpty());
    }

    private String createAdminToken() {
        User admin = userRepository.save(
                User.builder()
                        .userTag("adm-" + UUID.randomUUID().toString().substring(0, 8))
                        .nickname("admin")
                        .role(UserRole.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build()
        );
        return jwtProvider.createAccessToken(admin.getId(), "ADMIN");
    }
}
