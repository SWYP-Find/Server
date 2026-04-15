package com.swyp.picke.domain.share.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserProfile;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserProfileRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShareApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3PresignedUrlService s3PresignedUrlService;

    @Test
    @DisplayName("인증 사용자는 공유 키를 발급받고 비로그인 사용자는 그 키로 리캡을 조회할 수 있다")
    void recap_share_key_and_public_lookup_work() throws Exception {
        when(s3PresignedUrlService.generatePresignedUrl(anyString())).thenReturn("https://presigned-url");

        User user = userRepository.save(
                User.builder()
                        .userTag("user-" + UUID.randomUUID().toString().substring(0, 8))
                        .nickname("nickname")
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()
        );

        UserProfile profile = UserProfile.builder()
                .user(user)
                .nickname("recap-user")
                .characterType(CharacterType.OWL)
                .mannerTemperature(BigDecimal.valueOf(36.5))
                .build();
        profile.updatePhilosopherType(PhilosopherType.KANT);
        userProfileRepository.save(profile);

        String token = jwtProvider.createAccessToken(user.getId(), "USER");

        MvcResult shareKeyResult = mockMvc.perform(get("/api/v1/share/recap")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shareKey").isNotEmpty())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(shareKeyResult.getResponse().getContentAsByteArray(), Map.class);
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        String shareKey = (String) data.get("shareKey");

        mockMvc.perform(get("/api/v1/share/recap/{shareKey}", shareKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myCard.philosopherType").value("KANT"))
                .andExpect(jsonPath("$.data.preferenceReport.totalParticipation").value(0));
    }

    @Test
    @DisplayName("공유 키 발급 API는 인증이 필요하다")
    void recap_share_key_requires_authentication() throws Exception {
        mockMvc.perform(get("/api/v1/share/recap"))
                .andExpect(status().isUnauthorized());
    }
}
