package com.swyp.picke.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
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
    @DisplayName("광고 탭은 문서를 연 주소로 요청한다. 운영에서 열면 운영으로, dev에서 열면 dev로 나간다")
    void adGroup_defaultsToCurrentOrigin() throws Exception {
        mockMvc.perform(get("/v3/api-docs/" + AD_GROUP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andExpect(jsonPath("$.servers[*].url").value(hasItem("https://dev.picke.store")))
                .andExpect(jsonPath("$.servers[*].url").value(hasItem("https://picke.store")));
    }

    @Test
    @DisplayName("광고 탭에서 운영 광고 도메인을 고를 수 있다. 운영 광고는 별도 서비스라 API 서버로는 조회되지 않는다")
    void adGroup_offersAdDomain() throws Exception {
        mockMvc.perform(get("/v3/api-docs/" + AD_GROUP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[1].url").value("https://ad.picke.store"));
    }

    @Test
    @DisplayName("다른 탭의 서버 순서는 그대로 둔다")
    void otherGroups_keepProdFirst() throws Exception {
        mockMvc.perform(get("/v3/api-docs/0. 모든 API"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("https://picke.store"));
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
