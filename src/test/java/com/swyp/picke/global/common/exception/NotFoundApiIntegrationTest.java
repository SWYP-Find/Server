package com.swyp.picke.global.common.exception;

import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotFoundApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("매핑 없는 API 주소는 500 이 아니라 404 COMMON_404 로 응답한다")
    void unknownApi_returns404() throws Exception {
        User admin = userRepository.save(User.builder()
                .userTag("adm-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("admin")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        String token = jwtProvider.createAccessToken(admin.getId(), "ADMIN");

        mockMvc.perform(get("/api/v1/admin/not-exist-api").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMON_404"));
    }
}
