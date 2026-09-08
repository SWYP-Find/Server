package com.swyp.picke.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사용자 그룹은 FE_USED_OPERATIONS 화이트리스트로, 관리자 그룹은 pathsToExclude 로 광고를 걷어낸다.
 * 둘 중 하나만 손대도 광고 탭이 조용히 비어 버리므로 그룹 구성 자체를 고정해 둔다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerAdGroupTest {

    private static final String AD_GROUP = "3. 광고 API";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("광고 API는 스웨거 그룹 선택 목록에 별도 탭으로 나온다")
    void swaggerConfig_exposesAdGroup() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls[?(@.name == '" + AD_GROUP + "')]").exists());
    }

    @Test
    @DisplayName("광고 탭에는 앱용 조회·집계와 관리자용 소재 관리가 함께 묶인다")
    void adGroup_containsAppAndAdminOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs/" + AD_GROUP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/ads'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ads/impressions'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/ads'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/ads/stats'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/ads/clicks'].get").exists());
    }
}
