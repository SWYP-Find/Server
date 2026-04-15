package com.swyp.picke.domain.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.scenario.service.ScenarioAudioPipelineService;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminScenarioPublishFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BattleRepository battleRepository;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3PresignedUrlService s3PresignedUrlService;

    @MockitoBean
    private ScenarioAudioPipelineService scenarioAudioPipelineService;

    @Test
    void createScenario_pending_doesNotTriggerAudioPipeline() throws Exception {
        String adminToken = createAdminToken();
        Battle battle = createBattle();

        Map<String, Object> payload = scenarioPayload(battle.getId(), "PENDING");

        mockMvc.perform(post("/api/v1/admin/scenarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scenarioId").exists());

        verify(scenarioAudioPipelineService, never()).generateAndMergeAudioAsync(anyLong());
    }

    @Test
    void patchScenarioStatus_toPublished_triggersAudioPipeline() throws Exception {
        String adminToken = createAdminToken();
        Battle battle = createBattle();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/scenarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scenarioPayload(battle.getId(), "PENDING"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scenarioId").exists())
                .andReturn();

        Long scenarioId = extractId(createResult, "scenarioId");
        clearInvocations(scenarioAudioPipelineService);

        mockMvc.perform(patch("/api/v1/admin/scenarios/{scenarioId}", scenarioId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(scenarioAudioPipelineService, timeout(1000)).generateAndMergeAudioAsync(scenarioId);
    }

    private Map<String, Object> scenarioPayload(Long battleId, String status) {
        return Map.of(
                "battleId", battleId,
                "isInteractive", false,
                "status", status,
                "nodes", List.of(
                        Map.of(
                                "nodeName", "START",
                                "isStartNode", true,
                                "autoNextNode", "",
                                "scripts", List.of(
                                        Map.of(
                                                "speakerType", "NARRATOR",
                                                "speakerName", "Narrator",
                                                "text", "Opening script"
                                        )
                                ),
                                "interactiveOptions", List.of()
                        )
                ),
                "voiceSettings", Map.of("NARRATOR", "voice-narrator")
        );
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

    private Battle createBattle() {
        return battleRepository.save(
                Battle.builder()
                        .title("Scenario test battle")
                        .summary("summary")
                        .description("description")
                        .targetDate(LocalDate.now())
                        .audioDuration(30)
                        .status(BattleStatus.PENDING)
                        .creatorType(BattleCreatorType.ADMIN)
                        .build()
        );
    }

    private Long extractId(MvcResult result, String idField) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path(idField).asLong();
    }
}
