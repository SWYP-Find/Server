package com.swyp.picke.domain.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.notification.entity.Notification;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    @DisplayName("admin can create and list notices")
    void admin_can_create_and_list_notices() throws Exception {
        String adminToken = createAdminToken();
        String title = "notice-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> payload = Map.of(
                "category", "NOTICE",
                "title", title,
                "body", "maintenance at 22:00"
        );

        mockMvc.perform(post("/api/v1/admin/notices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").exists())
                .andExpect(jsonPath("$.data.category").value("NOTICE"))
                .andExpect(jsonPath("$.data.title").value(title));

        Notification saved = notificationRepository.findAll().stream()
                .filter(notification -> title.equals(notification.getTitle()))
                .findFirst()
                .orElseThrow();

        assertThat(saved.getUser()).isNull();
        assertThat(saved.getCategory()).isEqualTo(NotificationCategory.NOTICE);

        mockMvc.perform(get("/api/v1/admin/notices")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].title", hasItem(title)));
    }

    @Test
    @DisplayName("admin notice page form flow persists notice and user can fetch it")
    void admin_notice_page_form_flow_persists_notice_and_user_can_fetch_it() throws Exception {
        String adminToken = createAdminToken();
        String userToken = createUserToken();
        String marker = UUID.randomUUID().toString().substring(0, 8);
        String title = "ui-notice-" + marker;
        String body = "ui-body-" + marker;

        mockMvc.perform(get("/api/v1/admin/picke/notice"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"notice-form\"")))
                .andExpect(content().string(containsString("id=\"notice-title\"")))
                .andExpect(content().string(containsString("id=\"notice-body\"")))
                .andExpect(content().string(containsString("/js/admin/notice/notice.js")));

        Map<String, Object> payload = Map.of(
                "category", "NOTICE",
                "title", title,
                "body", body
        );

        String createResponse = mockMvc.perform(post("/api/v1/admin/notices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("NOTICE"))
                .andExpect(jsonPath("$.data.title").value(title))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long noticeId = objectMapper.readTree(createResponse)
                .path("data")
                .path("notificationId")
                .asLong();

        assertThat(noticeId).isPositive();

        Notification saved = notificationRepository.findById(noticeId).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo(title);
        assertThat(saved.getBody()).isEqualTo(body);
        assertThat(saved.getCategory()).isEqualTo(NotificationCategory.NOTICE);
        assertThat(saved.getUser()).isNull();

        mockMvc.perform(get("/api/v1/admin/notices")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("category", "NOTICE")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].title", hasItem(title)));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + userToken)
                        .param("category", "NOTICE")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].title", hasItem(title)));

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", noticeId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").value((int) noticeId))
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.body").value(body))
                .andExpect(jsonPath("$.data.category").value("NOTICE"));
    }

    @Test
    @DisplayName("admin registration creates CONTENT NOTICE EVENT broadcasts and user can list/detail them")
    void admin_registration_creates_all_categories_and_user_can_fetch_list_and_detail() throws Exception {
        String adminToken = createAdminToken();
        String userToken = createUserToken();
        String marker = UUID.randomUUID().toString().substring(0, 8);

        String contentTitle = "content-" + marker;
        String noticeTitle = "notice-" + marker;
        String eventTitle = "event-" + marker;

        Notification content = assertCategoryFlow(
                adminToken, userToken, NotificationCategory.CONTENT, contentTitle, NotificationDetailCode.NEW_BATTLE);
        Notification notice = assertCategoryFlow(
                adminToken, userToken, NotificationCategory.NOTICE, noticeTitle, NotificationDetailCode.POLICY_CHANGE);
        Notification event = assertCategoryFlow(
                adminToken, userToken, NotificationCategory.EVENT, eventTitle, NotificationDetailCode.PROMOTION);

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + userToken)
                        .param("category", "ALL")
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].title", hasItem(contentTitle)))
                .andExpect(jsonPath("$.data.items[*].title", hasItem(noticeTitle)))
                .andExpect(jsonPath("$.data.items[*].title", hasItem(eventTitle)));

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", content.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").value(content.getId().intValue()))
                .andExpect(jsonPath("$.data.category").value("CONTENT"));

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", notice.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").value(notice.getId().intValue()))
                .andExpect(jsonPath("$.data.category").value("NOTICE"));

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", event.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").value(event.getId().intValue()))
                .andExpect(jsonPath("$.data.category").value("EVENT"));
    }

    private Notification assertCategoryFlow(
            String adminToken,
            String userToken,
            NotificationCategory category,
            String title,
            NotificationDetailCode expectedDetailCode
    ) throws Exception {
        String body = "body-" + category.name();
        Map<String, Object> payload = Map.of(
                "category", category.name(),
                "title", title,
                "body", body
        );

        mockMvc.perform(post("/api/v1/admin/notices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value(category.name()))
                .andExpect(jsonPath("$.data.title").value(title));

        Notification saved = notificationRepository.findAll().stream()
                .filter(notification -> title.equals(notification.getTitle()))
                .findFirst()
                .orElseThrow();

        assertThat(saved.getUser()).isNull();
        assertThat(saved.getCategory()).isEqualTo(category);
        assertThat(saved.getDetailCode()).isEqualTo(expectedDetailCode);
        assertThat(saved.getBody()).isEqualTo(body);

        mockMvc.perform(get("/api/v1/admin/notices")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("category", category.name())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].title", hasItem(title)));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + userToken)
                        .param("category", category.name())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].title", hasItem(title)));

        return saved;
    }

    private String createAdminToken() {
        return createToken(UserRole.ADMIN, "adm");
    }

    private String createUserToken() {
        return createToken(UserRole.USER, "usr");
    }

    private String createToken(UserRole role, String prefix) {
        User user = userRepository.save(
                User.builder()
                        .userTag(prefix + "-" + UUID.randomUUID().toString().substring(0, 8))
                        .nickname(prefix)
                        .role(role)
                        .status(UserStatus.ACTIVE)
                        .build()
        );
        return jwtProvider.createAccessToken(user.getId(), role.name());
    }
}
