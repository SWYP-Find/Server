package com.swyp.picke.domain.classroom.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.repository.AiJobRepository;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassTopicDraftRepository;
import com.swyp.picke.domain.classroom.repository.ClassTopicVersionRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.service.report.ClassReportAiClient;
import com.swyp.picke.domain.classroom.service.topic.ClassTopicPrompts;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.repository.FundingOrderRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserProfile;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserBattleRepository;
import com.swyp.picke.domain.user.repository.UserProfileRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 검증된 주제 버전으로 커스텀 배틀 클래스를 만드는 흐름(#547)을 HTTP 로 검증한다. 주제 제안 AI 는 모킹한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassCustomBattleApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private FundingOrderRepository fundingOrderRepository;
    @Autowired private ClassTicketRepository classTicketRepository;
    @Autowired private ClassTopicDraftRepository draftRepository;
    @Autowired private ClassTopicVersionRepository versionRepository;
    @Autowired private AiJobRepository aiJobRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ClassMemberRepository classMemberRepository;
    @Autowired private BattleRepository battleRepository;
    @Autowired private BattleOptionRepository battleOptionRepository;
    @Autowired private BattleVoteRepository battleVoteRepository;
    @Autowired private UserBattleRepository userBattleRepository;

    @MockitoBean
    private ClassReportAiClient aiClient;

    private User owner;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        cleanUp();
        owner = newUser("선생님");
        ownerToken = tokenOf(owner, UserRole.USER);
        issueAndRegister(owner);
        when(aiClient.getModel()).thenReturn("test-model");
        when(aiClient.chatJson(eq(ClassTopicPrompts.SUGGEST_SYSTEM), anyString(), anyInt()))
                .thenReturn(new ClassReportAiClient.AiChatResult(candidatesJson(), "{}", 100, 200));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        battleVoteRepository.deleteAll();
        userBattleRepository.deleteAll();
        classMemberRepository.deleteAll();
        List<Long> customBattleIds = battleRepository.findAll().stream()
                .filter(b -> b.getCreatorType() == BattleCreatorType.USER).map(Battle::getId).toList();
        classroomRepository.deleteAll();
        versionRepository.deleteAll();
        draftRepository.deleteAll();
        aiJobRepository.deleteAll();
        battleOptionRepository.deleteAll(battleOptionRepository.findAll().stream()
                .filter(o -> customBattleIds.contains(o.getBattle().getId())).toList());
        battleRepository.deleteAllById(customBattleIds);
        classTicketRepository.deleteAll();
        fundingOrderRepository.deleteAll();
    }

    @Test
    @DisplayName("주제 버전으로 클래스 생성: CUSTOM 클래스 + 클래스 전용 배틀(비공개·USER)과 선택지 2개, 철학자 매핑 복사")
    void createCustomClass() throws Exception {
        long versionId = selectedVersion();

        JsonNode cls = data(createClass(Map.of("topicVersionId", versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.battleType").value("CUSTOM"))
                .andExpect(jsonPath("$.data.topicVersionId").value(versionId))
                .andExpect(jsonPath("$.data.battle.title").value("질문 2번은 무엇일까요?"))
                .andExpect(jsonPath("$.data.battle.summary").value("설명 2")));

        Battle battle = battleRepository.findById(cls.get("battle").get("battleId").asLong()).orElseThrow();
        assertThat(battle.getStatus()).isEqualTo(BattleStatus.PENDING);
        assertThat(battle.getCreatorType()).isEqualTo(BattleCreatorType.USER);
        assertThat(battle.getCreator().getId()).isEqualTo(owner.getId());
        assertThat(battle.getCategory()).isEqualTo(BattleCategory.PHILOSOPHY);
        assertThat(battle.getDifficulty()).isEqualTo(BattleDifficulty.INTERMEDIATE);
        assertThat(battle.isClassRecommendable()).isFalse();

        List<BattleOption> options = battleOptionRepository.findAll().stream()
                .filter(o -> o.getBattle().getId().equals(battle.getId()))
                .sorted(Comparator.comparing(BattleOption::getDisplayOrder)).toList();
        assertThat(options).extracting(BattleOption::getTitle).containsExactly("찬성 2", "반대 2");
        assertThat(options).extracting(BattleOption::getStance).containsExactly("이유 A2", "이유 B2");
        assertThat(options).extracting(BattleOption::getRepresentative).containsExactly("플라톤", "아리스토텔레스");
        assertThat(options).extracting(BattleOption::getImageUrl)
                .containsExactly("images/philosophers/plato.png", "images/philosophers/aristotle.png");

        // 앱 일반 상세·목록, 어드민 목록에 나오지 않는다
        mockMvc.perform(get("/api/v1/battles/" + battle.getId()).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
        String adminToken = tokenOf(newUser("관리자"), UserRole.ADMIN);
        String adminList = mockMvc.perform(get("/api/v1/admin/battles?size=100").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(adminList).get("data").get("items").findValuesAsText("battleId"))
                .doesNotContain(String.valueOf(battle.getId()));
        String pendingList = mockMvc.perform(get("/api/v1/admin/battles?size=100&status=PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(pendingList).get("data").get("items").findValuesAsText("battleId"))
                .doesNotContain(String.valueOf(battle.getId()));
    }

    @Test
    @DisplayName("커스텀 클래스 진행: 멤버 사전투표는 되고, TTS 완료는 409 CLASS_409_TTS")
    void customClassBattleFlow() throws Exception {
        JsonNode cls = data(createClass(Map.of("topicVersionId", selectedVersion(), "ownerParticipates", false)));
        long classId = cls.get("classId").asLong();
        long battleId = cls.get("battle").get("battleId").asLong();
        long optionA = battleOptionRepository.findAll().stream()
                .filter(o -> o.getBattle().getId().equals(battleId) && o.getDisplayOrder() == 1)
                .findFirst().orElseThrow().getId();

        String member = tokenOf(newUser("학생"), UserRole.USER);
        mockMvc.perform(post("/api/v1/classes/join").header("Authorization", "Bearer " + member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("inviteCode", cls.get("inviteCode").asText(), "displayName", "학생"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/classes/" + classId + "/battle/votes/pre").header("Authorization", "Bearer " + member)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("optionId", optionA))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/classes/" + classId + "/battle/votes/tts-complete").header("Authorization", "Bearer " + member))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_TTS"));
        mockMvc.perform(get("/api/v1/classes/" + classId + "/battle").header("Authorization", "Bearer " + member))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("요청 검사: battleId·topicVersionId 둘 다/둘 다 없음 400, 남의 주제 버전 404, 같은 초안 두 번째 클래스 409")
    void validation() throws Exception {
        long versionId = selectedVersion();

        createClass(Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_BATTLE_SOURCE"));
        createClass(Map.of("topicVersionId", versionId, "battleId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_BATTLE_SOURCE"));

        User other = newUser("다른 선생님");
        issueAndRegister(other);
        mockMvc.perform(post("/api/v1/classes").header("Authorization", "Bearer " + tokenOf(other, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json(classBody(Map.of("topicVersionId", versionId)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CLASS_404_TOPIC_VERSION"));

        createClass(Map.of("topicVersionId", versionId)).andExpect(status().isOk());
        long draftId = versionRepository.findById(versionId).orElseThrow().getDraft().getId();
        long otherVersion = data(mockMvc.perform(post("/api/v1/class-topic-drafts/" + draftId + "/selection")
                .header("Authorization", "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("batchNo", 1, "candidateId", 3))))).get("topicVersionId").asLong();
        createClass(Map.of("topicVersionId", otherVersion))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_TOPIC_DRAFT_USED"));
    }

    // ===== 헬퍼 =====

    /** 초안 생성 → 제안(모킹) → 2번 후보 선택 → 주제 버전 ID */
    private long selectedVersion() throws Exception {
        long draftId = data(mockMvc.perform(post("/api/v1/class-topic-drafts").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("topicSeed", "감정 표현의 자유", "difficulty", "INTERMEDIATE", "categories", List.of("철학"))))))
                .get("draftId").asLong();
        mockMvc.perform(post("/api/v1/class-topic-drafts/" + draftId + "/suggestions").header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.data.suggestStatus").value("SUCCEEDED"));
        return data(mockMvc.perform(post("/api/v1/class-topic-drafts/" + draftId + "/selection")
                .header("Authorization", "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("batchNo", 1, "candidateId", 2))))).get("topicVersionId").asLong();
    }

    private ResultActions createClass(Map<String, Object> source) throws Exception {
        return mockMvc.perform(post("/api/v1/classes").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content(json(classBody(source))));
    }

    private static Map<String, Object> classBody(Map<String, Object> source) {
        Map<String, Object> body = new HashMap<>(Map.of("name", "커스텀 클래스", "commentRequired", false, "ownerParticipates", true));
        body.putAll(source);
        return body;
    }

    private static Map<String, Object> candidatesJson() {
        List<String> philosophers = List.of("SOCRATES", "PLATO", "ARISTOTLE", "KANT");
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            candidates.add(Map.of(
                    "question", "질문 " + i + "번은 무엇일까요?",
                    "description", "설명 " + i,
                    "category", "PHILOSOPHY",
                    "difficulty", "INTERMEDIATE",
                    "options", List.of(
                            Map.of("label", "찬성 " + i, "rationale", "이유 A" + i, "profile_id", philosophers.get(i - 1)),
                            Map.of("label", "반대 " + i, "rationale", "이유 B" + i, "profile_id", philosophers.get(i)))));
        }
        return Map.of("needs_clarification", false, "candidates", candidates);
    }

    private JsonNode data(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("data");
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private User newUser(String nickname) {
        User user = userRepository.save(User.builder()
                .userTag("cc-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        userProfileRepository.save(UserProfile.builder()
                .user(user).nickname(nickname).characterType(CharacterType.OWL)
                .mannerTemperature(BigDecimal.valueOf(36.5))
                .build());
        return user;
    }

    private String tokenOf(User user, UserRole role) {
        return jwtProvider.createAccessToken(user.getId(), role.name());
    }

    private void issueAndRegister(User user) {
        FundingOrder order = fundingOrderRepository.save(FundingOrder.builder()
                .platform(FundingPlatform.TUMBLBUG)
                .campaignId("camp-test")
                .orderId("order-" + UUID.randomUUID())
                .rewardTier(RewardTier.SEAT_5)
                .quantity(1)
                .supporterName("후원자")
                .paymentStatus(PaymentStatus.PAID_CONFIRMED)
                .paidAt(LocalDateTime.now().minusDays(3))
                .build());
        ClassTicket ticket = ClassTicket.issue(order, "hash-" + UUID.randomUUID());
        ticket.register(user, LocalDateTime.now().minusDays(1));
        classTicketRepository.save(ticket);
    }
}
