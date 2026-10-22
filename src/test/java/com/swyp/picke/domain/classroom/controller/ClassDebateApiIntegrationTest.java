package com.swyp.picke.domain.classroom.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.entity.AiJob;
import com.swyp.picke.domain.classroom.enums.AiJobStatus;
import com.swyp.picke.domain.classroom.enums.AiJobType;
import com.swyp.picke.domain.classroom.repository.AiJobRepository;
import com.swyp.picke.domain.classroom.repository.ClassDebateMessageRepository;
import com.swyp.picke.domain.classroom.repository.ClassDebateSessionRepository;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassTopicDraftRepository;
import com.swyp.picke.domain.classroom.repository.ClassTopicVersionRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.service.debate.ClassDebatePrompts;
import com.swyp.picke.domain.classroom.service.debate.ClassDebateService;
import com.swyp.picke.domain.classroom.service.report.ClassModerationClient;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.ResourceAccessException;

/**
 * 커스텀 배틀 AI 철학자 3라운드 토론(#548, PRD §8·§11, 부록 A)을 HTTP 로 검증한다. 주제 제안·토론 AI 는 모킹한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassDebateApiIntegrationTest {

    private static final Pattern ROUND = Pattern.compile("\\[라운드] (\\d) / 3");

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
    @Autowired private ClassDebateSessionRepository sessionRepository;
    @Autowired private ClassDebateMessageRepository messageRepository;
    @Autowired private ClassDebateService debateService;

    @MockitoBean
    private ClassReportAiClient aiClient;
    @MockitoBean
    private ClassModerationClient moderationClient;

    private String ownerToken;
    private String memberToken;
    private long classId;
    private long optionA;
    private long optionB;

    @BeforeEach
    void setUp() throws Exception {
        cleanUp();
        ReflectionTestUtils.setField(debateService, "retryBackoffMillis", 0L);
        User owner = newUser("선생님");
        ownerToken = tokenOf(owner);
        issueAndRegister(owner);
        when(aiClient.getModel()).thenReturn("test-model");
        when(moderationClient.flaggedCategories(anyString())).thenReturn(Set.of());
        when(aiClient.chatJson(eq(ClassTopicPrompts.SUGGEST_SYSTEM), anyString(), anyInt()))
                .thenReturn(result(candidatesJson()));
        stubDebate();

        JsonNode cls = createCustomClass();
        classId = cls.get("classId").asLong();
        long battleId = cls.get("battle").get("battleId").asLong();
        for (BattleOption option : battleOptionRepository.findAll()) {
            if (option.getBattle().getId().equals(battleId)) {
                if (option.getDisplayOrder() == 1) {
                    optionA = option.getId();
                } else {
                    optionB = option.getId();
                }
            }
        }
        memberToken = tokenOf(newUser("학생"));
        mockMvc.perform(post("/api/v1/classes/join").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("inviteCode", cls.get("inviteCode").asText(), "displayName", "학생"))))
                .andExpect(status().isOk());
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        messageRepository.deleteAll();
        sessionRepository.deleteAll();
        aiJobRepository.deleteAll();
        battleVoteRepository.deleteAll();
        userBattleRepository.deleteAll();
        classMemberRepository.deleteAll();
        List<Long> customBattleIds = battleRepository.findAll().stream()
                .filter(b -> b.getCreatorType() == BattleCreatorType.USER).map(Battle::getId).toList();
        classroomRepository.deleteAll();
        versionRepository.deleteAll();
        draftRepository.deleteAll();
        battleOptionRepository.deleteAll(battleOptionRepository.findAll().stream()
                .filter(o -> customBattleIds.contains(o.getBattle().getId())).toList());
        battleRepository.deleteAllById(customBattleIds);
        classTicketRepository.deleteAll();
        fundingOrderRepository.deleteAll();
    }

    @Test
    @DisplayName("3라운드 전체 흐름: 반대 선택지·철학자 고정, 라운드마다 AI 주장 → 반박, 완료 후 사후투표, 시작 후 사전투표 잠금")
    void fullDebate() throws Exception {
        preVote(optionA).andExpect(status().isOk());
        mockMvc.perform(get(debate()).header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CLASS_404_DEBATE"));

        start().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_USER"))
                .andExpect(jsonPath("$.data.round").value(1))
                .andExpect(jsonPath("$.data.canReply").value(true))
                .andExpect(jsonPath("$.data.participantOption.optionId").value(optionA))
                .andExpect(jsonPath("$.data.participantOption.position").value("A"))
                .andExpect(jsonPath("$.data.opponentOption.optionId").value(optionB))
                .andExpect(jsonPath("$.data.philosopher.code").value("ARISTOTLE"))
                .andExpect(jsonPath("$.data.philosopher.name").value("아리스토텔레스"))
                .andExpect(jsonPath("$.data.aiNotice").exists())
                .andExpect(jsonPath("$.data.messages", hasSize(1)))
                .andExpect(jsonPath("$.data.messages[0].speaker").value("AI"))
                .andExpect(jsonPath("$.data.messages[0].focus").value("EVIDENCE"))
                .andExpect(jsonPath("$.data.messages[0].focusLabel").value("근거"));
        verify(aiClient).chatJson(eq(ClassDebatePrompts.SYSTEM),
                argThat(p -> p.contains("[라운드] 1 / 3") && p.contains("(아직 없음)")
                        && p.contains("[상대 입장 - 당신이 대변] B. 반대 2") && p.contains("[철학자] 아리스토텔레스")),
                anyInt());

        // 시작 후에는 사전투표를 바꿀 수 없고, 토론 전 사후투표도 막힌다
        preVote(optionB).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_PRE_VOTE_LOCKED"));
        postVote(optionB).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_DEBATE_INCOMPLETE"));

        JsonNode r1 = data(reply(1, longText("첫 번째 반박"), "key-1")
                .andExpect(jsonPath("$.data.status").value("WAITING_USER"))
                .andExpect(jsonPath("$.data.round").value(2))
                .andExpect(jsonPath("$.data.messages", hasSize(3))));
        long userMessage1 = r1.get("messages").get(1).get("messageId").asLong();
        assertThat(r1.get("messages").get(2).get("replyToMessageId").asLong()).isEqualTo(userMessage1);
        verify(aiClient).chatJson(eq(ClassDebatePrompts.SYSTEM),
                argThat(p -> p.contains("[라운드] 2 / 3") && p.contains("(1라운드 참가자 반박)") && p.contains("첫 번째 반박")),
                anyInt());

        reply(2, longText("두 번째 반박"), "key-2").andExpect(jsonPath("$.data.round").value(3));
        reply(3, longText("세 번째 반박"), "key-3")
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.canReply").value(false))
                .andExpect(jsonPath("$.data.messages", hasSize(6)))
                .andExpect(jsonPath("$.data.messages[5].speaker").value("USER"));

        postVote(optionB).andExpect(status().isOk());
        assertThat(aiJobRepository.findAll()).filteredOn(j -> j.getType() == AiJobType.DEBATE_ARGUMENT)
                .hasSize(3)
                .allMatch(j -> j.getStatus() == AiJobStatus.SUCCEEDED && j.getAttempt() == 1 && j.getMember() != null);

        // 재접속은 저장된 대화 그대로, 다시 시작해도 새 세션을 만들지 않는다
        start().andExpect(jsonPath("$.data.messages", hasSize(6)));
        assertThat(sessionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("반박 중복·검사: 같은 키 재전송은 그대로, 다른 키 409, 다음 라운드 미리 제출 409, 50자 미만 400")
    void replyIdempotency() throws Exception {
        preVote(optionB);
        start().andExpect(jsonPath("$.data.philosopher.code").value("PLATO"));

        reply(1, "너무 짧아요", "key-short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_DEBATE_REPLY"));
        reply(2, longText("미리 쓴 반박"), "key-x")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_DEBATE_STATE"));

        reply(1, longText("첫 번째 반박"), "key-1").andExpect(jsonPath("$.data.round").value(2));
        reply(1, longText("첫 번째 반박"), "key-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.round").value(2))
                .andExpect(jsonPath("$.data.messages", hasSize(3)));
        reply(1, longText("다른 내용"), "key-other")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_DEBATE_ROUND"));
    }

    @Test
    @DisplayName("반박 차단: 개인정보·위협 표현은 저장하지 않고 400, 라운드를 쓰지 않아 고친 반박은 그대로 1라운드로 제출된다")
    void replyBlocked() throws Exception {
        preVote(optionB);
        start();

        reply(1, longText("제 번호는 010-1234-5678 이에요"), "key-phone")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_DEBATE_PERSONAL_INFO"));
        verify(moderationClient, never()).flaggedCategories(anyString());

        String threat = longText("위협");
        when(moderationClient.flaggedCategories(threat)).thenReturn(Set.of("harassment", "harassment/threatening"));
        reply(1, threat, "key-threat")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_DEBATE_BLOCKED"));

        // 폭력을 다루는 정상 논증(violence 만 걸림)은 막지 않는다
        String violence = longText("폭력 논증");
        when(moderationClient.flaggedCategories(violence)).thenReturn(Set.of("violence"));
        assertThat(messageRepository.findAll()).hasSize(1);
        reply(1, violence, "key-violence")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.round").value(2));
    }

    @Test
    @DisplayName("Moderation 장애면 반박을 막지 않는다")
    void replyAllowedWhenModerationFails() throws Exception {
        preVote(optionB);
        start();
        when(moderationClient.flaggedCategories(anyString())).thenThrow(new IllegalStateException("down"));

        reply(1, longText("첫 번째 반박"), "key-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.round").value(2));
    }

    @Test
    @DisplayName("AI 장애: 3회 실패하면 FAILED(반박 불가·재시도 가능), 재시도는 같은 작업으로 성공")
    void failureAndRetry() throws Exception {
        preVote(optionA);
        when(aiClient.chatJson(eq(ClassDebatePrompts.SYSTEM), anyString(), anyInt()))
                .thenThrow(new ResourceAccessException("timeout"));

        start().andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.canReply").value(false))
                .andExpect(jsonPath("$.data.canRetry").value(true))
                .andExpect(jsonPath("$.data.messages", hasSize(0)));
        reply(1, longText("반박"), "key-1").andExpect(status().isConflict());

        stubDebate();
        mockMvc.perform(post(debate() + "/retry").header("Authorization", "Bearer " + memberToken))
                .andExpect(jsonPath("$.data.status").value("WAITING_USER"))
                .andExpect(jsonPath("$.data.messages", hasSize(1)));
        mockMvc.perform(post(debate() + "/retry").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_DEBATE_RETRY"));

        AiJob job = aiJobRepository.findAll().stream().filter(j -> j.getType() == AiJobType.DEBATE_ARGUMENT).findFirst().orElseThrow();
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
        assertThat(job.getAttempt()).isEqualTo(4);
    }

    @Test
    @DisplayName("응답 검사: round 필드가 있으면 1회 보정, 길이 위반이 계속되면 REJECTED")
    void argumentValidation() throws Exception {
        preVote(optionA);
        Map<String, Object> withRound = new HashMap<>(argument(1));
        withRound.put("round", 2);
        when(aiClient.chatJson(eq(ClassDebatePrompts.SYSTEM), anyString(), anyInt()))
                .thenReturn(result(withRound))
                .thenReturn(result(argument(1)));
        start().andExpect(jsonPath("$.data.status").value("WAITING_USER"));
        verify(aiClient).chatJson(eq(ClassDebatePrompts.SYSTEM), argThat(p -> p.contains("[고칠 점]") && p.contains("round 필드")), anyInt());

        when(aiClient.chatJson(eq(ClassDebatePrompts.SYSTEM), anyString(), anyInt()))
                .thenReturn(result(Map.of("argument_text", "너무 짧은 주장이에요.", "focus", "EVIDENCE")));
        reply(1, longText("첫 번째 반박"), "key-1")
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.round").value(2));
        AiJob job = aiJobRepository.findByIdempotencyKey(jobKey(2)).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.REJECTED);
        assertThat(job.getErrorCode()).isEqualTo("ARGUMENT_LENGTH");
        assertThat(job.getAttempt()).isEqualTo(2);
    }

    @Test
    @DisplayName("접근: 사전투표 전 시작 409, 참여하지 않은 운영자 403")
    void access() throws Exception {
        start().andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_DEBATE_PRE_VOTE"));
        mockMvc.perform(post(debate()).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    // ===== AI 응답 =====

    /** 프롬프트의 라운드를 읽어 그 라운드에 맞는 정상 주장을 돌려준다 */
    private void stubDebate() {
        when(aiClient.chatJson(eq(ClassDebatePrompts.SYSTEM), anyString(), anyInt()))
                .thenAnswer(inv -> {
                    Matcher m = ROUND.matcher(inv.getArgument(1));
                    int round = m.find() ? Integer.parseInt(m.group(1)) : 0;
                    return result(argument(round));
                });
    }

    private static Map<String, Object> argument(int round) {
        return Map.of(
                "argument_text", round + "라운드 주장이에요. 감정과 표현이 다를 수 있다는 점은 인정하지만, 관계 속에서는 상대가 확인할 수 있는 표현도 중요하지 않을까요?",
                "focus", "EVIDENCE");
    }

    private static ClassReportAiClient.AiChatResult result(Map<String, Object> json) {
        return new ClassReportAiClient.AiChatResult(json, "{\"raw\":true}", 300, 150);
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

    // ===== 헬퍼 =====

    /** 2번 후보(A=플라톤, B=아리스토텔레스)로 운영자 미참여 커스텀 클래스를 만든다 */
    private JsonNode createCustomClass() throws Exception {
        long draftId = data(mockMvc.perform(post("/api/v1/class-topic-drafts").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("topicSeed", "감정 표현의 자유", "difficulty", "INTERMEDIATE", "categories", List.of("철학"))))))
                .get("draftId").asLong();
        mockMvc.perform(post("/api/v1/class-topic-drafts/" + draftId + "/suggestions").header("Authorization", "Bearer " + ownerToken));
        long versionId = data(mockMvc.perform(post("/api/v1/class-topic-drafts/" + draftId + "/selection")
                .header("Authorization", "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("batchNo", 1, "candidateId", 2))))).get("topicVersionId").asLong();
        return data(mockMvc.perform(post("/api/v1/classes").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "토론 클래스", "topicVersionId", versionId,
                        "commentRequired", false, "ownerParticipates", false)))));
    }

    private String debate() {
        return "/api/v1/classes/" + classId + "/battle/debate";
    }

    private String jobKey(int round) {
        return "DEBATE_ARGUMENT:" + sessionRepository.findAll().get(0).getId() + ":" + round;
    }

    private ResultActions start() throws Exception {
        return mockMvc.perform(post(debate()).header("Authorization", "Bearer " + memberToken));
    }

    private ResultActions reply(int round, String content, String key) throws Exception {
        return mockMvc.perform(post(debate() + "/replies").header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("roundIndex", round, "content", content, "requestKey", key))));
    }

    private ResultActions preVote(long optionId) throws Exception {
        return mockMvc.perform(post("/api/v1/classes/" + classId + "/battle/votes/pre").header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("optionId", optionId))));
    }

    private ResultActions postVote(long optionId) throws Exception {
        return mockMvc.perform(post("/api/v1/classes/" + classId + "/battle/votes/post").header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("optionId", optionId))));
    }

    /** 50자 이상이 되도록 앞에 표지를 붙인 반박 */
    private static String longText(String marker) {
        return marker + ": 속으로 슬퍼해도 밖으로 표현하지 않을 수 있어요. 표현 방식은 사람마다 달라서 겉모습만으로 감정을 판단하면 안 된다고 생각해요.";
    }

    private JsonNode data(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("data");
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private User newUser(String nickname) {
        User user = userRepository.save(User.builder()
                .userTag("cd-" + UUID.randomUUID().toString().substring(0, 8))
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

    private String tokenOf(User user) {
        return jwtProvider.createAccessToken(user.getId(), UserRole.USER.name());
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
