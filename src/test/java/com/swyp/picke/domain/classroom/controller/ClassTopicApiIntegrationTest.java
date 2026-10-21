package com.swyp.picke.domain.classroom.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.classroom.entity.AiJob;
import com.swyp.picke.domain.classroom.enums.AiJobStatus;
import com.swyp.picke.domain.classroom.enums.AiJobType;
import com.swyp.picke.domain.classroom.repository.AiJobRepository;
import com.swyp.picke.domain.classroom.repository.ClassTopicDraftRepository;
import com.swyp.picke.domain.classroom.repository.ClassTopicVersionRepository;
import com.swyp.picke.domain.classroom.service.report.ClassReportAiClient;
import com.swyp.picke.domain.classroom.service.topic.ClassTopicAiService;
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
import com.swyp.picke.domain.user.repository.UserProfileRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.ResourceAccessException;

/**
 * 커스텀 배틀 AI 주제 제안·선택·수정 검증(PRD §10, 부록 A, A01~A03)을 HTTP 로 검증한다. AI 제공자는 모킹한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassTopicApiIntegrationTest {

    private static final String BASE = "/api/v1/class-topic-drafts";
    private static final List<String> PHILOSOPHERS = List.of(
            "SOCRATES", "PLATO", "ARISTOTLE", "KANT", "NIETZSCHE", "MARX", "SARTRE", "CONFUCIUS", "LAOZI", "BUDDHA");

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
    @Autowired private ClassTopicAiService aiService;

    @MockitoBean
    private ClassReportAiClient aiClient;

    private String ownerToken;

    @BeforeEach
    void setUp() {
        cleanUp();
        ReflectionTestUtils.setField(aiService, "retryBackoffMillis", 0L);
        User owner = newUser("선생님");
        ownerToken = tokenOf(owner);
        issueAndRegister(owner);
        when(aiClient.getModel()).thenReturn("test-model");
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        versionRepository.deleteAll();
        draftRepository.deleteAll();
        aiJobRepository.deleteAll();
        classTicketRepository.deleteAll();
        fundingOrderRepository.deleteAll();
    }

    @Test
    @DisplayName("초안 생성 → 제안: 후보 8개 저장, 남은 횟수 2, ai_jobs 성공·토큰 기록, 재조회는 AI 없이 같은 결과")
    void suggestSuccess() throws Exception {
        stubSuggest(candidatesJson(8, "INTERMEDIATE", "PHILOSOPHY"));
        long draftId = createDraft();

        suggest(draftId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.batchNo").value(1))
                .andExpect(jsonPath("$.data.remainingSuggestions").value(2))
                .andExpect(jsonPath("$.data.candidates", hasSize(8)))
                .andExpect(jsonPath("$.data.candidates[0].candidateId").value(1))
                .andExpect(jsonPath("$.data.candidates[0].category").value("철학"))
                .andExpect(jsonPath("$.data.candidates[0].options[0].position").value("A"))
                .andExpect(jsonPath("$.data.candidates[0].options[0].philosopher").value("SOCRATES"))
                .andExpect(jsonPath("$.data.candidates[0].options[0].philosopherName").value("소크라테스"));

        AiJob job = onlyJob(AiJobType.TOPIC_CANDIDATES);
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
        assertThat(job.getAttempt()).isEqualTo(1);
        assertThat(job.getInputTokens()).isEqualTo(500);
        assertThat(job.getModel()).isEqualTo("test-model");
        assertThat(job.getTicket()).isNotNull();

        mockMvc.perform(get(BASE + "/" + draftId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidates", hasSize(8)));
        verify(aiClient, times(1)).chatJson(eq(ClassTopicPrompts.SUGGEST_SYSTEM), anyString(), anyInt());
    }

    @Test
    @DisplayName("규칙 위반(7개·허용 목록 밖 철학자) → 문제 목록으로 1회 보정 → 성공, attempt 2")
    void suggestCorrectedOnce() throws Exception {
        Map<String, Object> bad = candidatesJson(7, "INTERMEDIATE", "PHILOSOPHY");
        firstOption(bad).put("profile_id", "HEGEL");
        when(aiClient.chatJson(eq(ClassTopicPrompts.SUGGEST_SYSTEM), anyString(), anyInt()))
                .thenReturn(result(bad))
                .thenReturn(result(candidatesJson(8, "INTERMEDIATE", "PHILOSOPHY")));
        long draftId = createDraft();

        suggest(draftId, null)
                .andExpect(jsonPath("$.data.suggestStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.candidates", hasSize(8)));

        verify(aiClient).chatJson(eq(ClassTopicPrompts.SUGGEST_SYSTEM),
                argThat(prompt -> prompt.contains("[고칠 점]") && prompt.contains("정확히 8개")
                        && prompt.contains("profile_id 는 [철학자 목록]의 id")),
                anyInt());
        AiJob job = onlyJob(AiJobType.TOPIC_CANDIDATES);
        assertThat(job.getAttempt()).isEqualTo(2);
        assertThat(job.getInputTokens()).isEqualTo(1000);
    }

    @Test
    @DisplayName("보정 후에도 위반 → FAILED, 기존 후보·횟수 유지, job REJECTED")
    void suggestRejectedKeepsPreviousCandidates() throws Exception {
        when(aiClient.chatJson(eq(ClassTopicPrompts.SUGGEST_SYSTEM), anyString(), anyInt()))
                .thenReturn(result(candidatesJson(8, "INTERMEDIATE", "PHILOSOPHY")))
                .thenReturn(result(candidatesJson(8, "BEGINNER", "PHILOSOPHY")))
                .thenReturn(result(candidatesJson(8, "BEGINNER", "PHILOSOPHY")));
        long draftId = createDraft();
        suggest(draftId, null).andExpect(jsonPath("$.data.batchNo").value(1));
        String firstQuestion = candidateQuestion(draftId, 0);

        suggest(draftId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.batchNo").value(1))
                .andExpect(jsonPath("$.data.remainingSuggestions").value(2))
                .andExpect(jsonPath("$.data.candidates[0].question").value(firstQuestion));

        AiJob failed = aiJobRepository.findByIdempotencyKey("TOPIC_CANDIDATES:" + draftId + ":2").orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(AiJobStatus.REJECTED);
        assertThat(failed.getErrorCode()).isEqualTo("CANDIDATE_RULES");
        assertThat(failed.getAttempt()).isEqualTo(2);
    }

    @Test
    @DisplayName("맥락 부족 → NEEDS_CLARIFICATION + 안내 문구, 후보 없음, 횟수 차감 없음")
    void suggestNeedsClarification() throws Exception {
        Map<String, Object> json = new HashMap<>();
        json.put("needs_clarification", true);
        json.put("clarification_message", "어떤 작품의 어떤 장면인지 적어 주세요.");
        json.put("candidates", List.of());
        stubSuggest(json);
        long draftId = createDraft();

        suggest(draftId, null)
                .andExpect(jsonPath("$.data.suggestStatus").value("NEEDS_CLARIFICATION"))
                .andExpect(jsonPath("$.data.clarificationMessage").value("어떤 작품의 어떤 장면인지 적어 주세요."))
                .andExpect(jsonPath("$.data.candidates", hasSize(0)))
                .andExpect(jsonPath("$.data.remainingSuggestions").value(3));
    }

    @Test
    @DisplayName("제공자 장애 → 3회까지 재시도 후 FAILED(job FAILED), 횟수 차감 없음")
    void suggestProviderFailure() throws Exception {
        when(aiClient.chatJson(eq(ClassTopicPrompts.SUGGEST_SYSTEM), anyString(), anyInt()))
                .thenThrow(new ResourceAccessException("timeout"));
        long draftId = createDraft();

        suggest(draftId, null)
                .andExpect(jsonPath("$.data.suggestStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.remainingSuggestions").value(3));
        verify(aiClient, times(3)).chatJson(eq(ClassTopicPrompts.SUGGEST_SYSTEM), anyString(), anyInt());
        AiJob job = onlyJob(AiJobType.TOPIC_CANDIDATES);
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.FAILED);
        assertThat(job.getAttempt()).isEqualTo(3);
    }

    @Test
    @DisplayName("조건 변경 재추천은 새 배치로 세고 조건을 바꾼다. 분당 2회 초과 429, 성공 3배치 후 409")
    void suggestQuotaAndRateLimit() throws Exception {
        when(aiClient.chatJson(eq(ClassTopicPrompts.SUGGEST_SYSTEM), anyString(), anyInt()))
                .thenReturn(result(candidatesJson(8, "INTERMEDIATE", "PHILOSOPHY")))
                .thenReturn(result(candidatesJson(8, "ADVANCED", "SOCIETY")))
                .thenReturn(result(candidatesJson(8, "ADVANCED", "SOCIETY")));
        long draftId = createDraft();
        suggest(draftId, null).andExpect(jsonPath("$.data.batchNo").value(1));
        suggest(draftId, Map.of("difficulty", "ADVANCED", "categories", List.of("사회")))
                .andExpect(jsonPath("$.data.batchNo").value(2))
                .andExpect(jsonPath("$.data.difficulty").value("ADVANCED"))
                .andExpect(jsonPath("$.data.categories[0]").value("사회"));

        suggest(draftId, null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("CLASS_429_TOPIC"));

        ageJobs();
        suggest(draftId, null).andExpect(jsonPath("$.data.batchNo").value(3))
                .andExpect(jsonPath("$.data.remainingSuggestions").value(0));
        ageJobs();
        suggest(draftId, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_TOPIC_SUGGEST_LIMIT"));
    }

    @Test
    @DisplayName("후보 선택: 다른 배치 번호 409, 선택하면 CANDIDATE 버전, 같은 후보 재선택은 같은 버전")
    void selectCandidate() throws Exception {
        stubSuggest(candidatesJson(8, "INTERMEDIATE", "PHILOSOPHY"));
        long draftId = createDraft();
        suggest(draftId, null);

        select(draftId, 2, 3)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_TOPIC_CANDIDATE"));
        select(draftId, 1, 9).andExpect(status().isNotFound());

        long versionId = data(select(draftId, 1, 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("CANDIDATE"))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andExpect(jsonPath("$.data.question").value("질문 3번은 무엇일까요?"))
                .andExpect(jsonPath("$.data.options[1].philosopher").value("KANT")))
                .get("topicVersionId").asLong();
        select(draftId, 1, 3).andExpect(jsonPath("$.data.topicVersionId").value(versionId));

        mockMvc.perform(get(BASE + "/" + draftId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.data.latestVersion.topicVersionId").value(versionId));
        verify(aiClient, never()).chatJson(eq(ClassTopicPrompts.VALIDATE_SYSTEM), anyString(), anyInt());
    }

    @Test
    @DisplayName("수정 검증: 형식 오류·변경 없음은 AI 없이 처리, 통과 시 EDITED 버전, 불통과는 issues 만, 장애는 503 + 횟수 환불")
    void validateEdits() throws Exception {
        stubSuggest(candidatesJson(8, "INTERMEDIATE", "PHILOSOPHY"));
        long draftId = createDraft();
        suggest(draftId, null);
        long baseId = data(select(draftId, 1, 1)).get("topicVersionId").asLong();

        // 1) 형식 오류: AI 호출 없이 필드별 issues, 횟수 그대로
        validate(draftId, baseId, "가".repeat(61), "설명", "판단할 수 있다", "판단할 수 있다", null)
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.issues[0].field").value("question"))
                .andExpect(jsonPath("$.data.issues[0].code").value("TOO_LONG"))
                .andExpect(jsonPath("$.data.issues[1].code").value("DUPLICATE_OPTION"))
                .andExpect(jsonPath("$.data.remainingValidations").value(3));

        // 2) 바탕 버전과 같으면 그대로 통과
        validate(draftId, baseId, "질문 1번은 무엇일까요?", "설명 1", "찬성 1", "반대 1", null)
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.validatedTopicVersionId").value(baseId))
                .andExpect(jsonPath("$.data.remainingValidations").value(3));
        verify(aiClient, never()).chatJson(eq(ClassTopicPrompts.VALIDATE_SYSTEM), anyString(), anyInt());

        // 3) AI 불통과: 새 버전 없음, 이유만 (A02)
        when(aiClient.chatJson(eq(ClassTopicPrompts.VALIDATE_SYSTEM), anyString(), anyInt()))
                .thenReturn(result(Map.of("valid", false, "issues", List.of(Map.of(
                        "field", "options[0].label", "code", "OPTION_MISMATCH_QUESTION",
                        "message", "선택지가 바뀐 질문에 대한 답이 아니에요.")))))
                .thenReturn(result(Map.of("valid", true, "issues", List.of())))
                .thenThrow(new ResourceAccessException("timeout"));
        validate(draftId, baseId, "감정을 숨기는 것도 거짓말일까요?", "설명 1", "찬성 1", "반대 1", null)
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.issues[0].field").value("options[0].label"))
                .andExpect(jsonPath("$.data.issues[0].code").value("OPTION_MISMATCH_QUESTION"))
                .andExpect(jsonPath("$.data.validatedTopicVersionId").doesNotExist())
                .andExpect(jsonPath("$.data.remainingValidations").value(2));

        // 4) AI 통과: 철학자를 바꾼 EDITED 버전 생성, 입력 그대로 보존
        validate(draftId, baseId, "감정을 숨기는 것도 거짓말일까요?", "감정 표현과 정직의 관계를 따져 봐요.",
                "거짓말이다", "거짓말이 아니다", "KANT")
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.version.source").value("EDITED"))
                .andExpect(jsonPath("$.data.version.versionNo").value(2))
                .andExpect(jsonPath("$.data.version.question").value("감정을 숨기는 것도 거짓말일까요?"))
                .andExpect(jsonPath("$.data.version.options[0].philosopher").value("KANT"))
                .andExpect(jsonPath("$.data.version.options[1].philosopher").value("PLATO"))
                .andExpect(jsonPath("$.data.remainingValidations").value(1));

        // 5) 제공자 장애: 503, 횟수 환불
        validate(draftId, baseId, "다른 질문은 무엇일까요?", "설명 1", "찬성 1", "반대 1", null)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("CLASS_503_TOPIC_AI"));
        mockMvc.perform(get(BASE + "/" + draftId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.data.remainingValidations").value(1))
                .andExpect(jsonPath("$.data.latestVersion.versionNo").value(2));
    }

    @Test
    @DisplayName("권한: 이용권 없으면 403, 다른 사람의 초안은 404, 조건 위반 400")
    void access() throws Exception {
        String stranger = tokenOf(newUser("외부인"));
        mockMvc.perform(post(BASE).header("Authorization", "Bearer " + stranger)
                        .contentType(MediaType.APPLICATION_JSON).content(json(draftBody("감정 표현의 자유"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_TICKET"));

        mockMvc.perform(post(BASE).header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json(draftBody("가"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_TOPIC_CONDITION"));

        long draftId = createDraft();
        mockMvc.perform(get(BASE + "/" + draftId).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CLASS_404_TOPIC_DRAFT"));
        mockMvc.perform(post(BASE + "/" + draftId + "/suggestions").header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
    }

    // ===== AI 응답 =====

    private void stubSuggest(Map<String, Object> json) {
        when(aiClient.chatJson(eq(ClassTopicPrompts.SUGGEST_SYSTEM), anyString(), anyInt())).thenReturn(result(json));
    }

    private static ClassReportAiClient.AiChatResult result(Map<String, Object> json) {
        return new ClassReportAiClient.AiChatResult(json, "{\"raw\":true}", 500, 900);
    }

    /** 후보 n 개. i 번째 후보의 선택지 철학자는 목록의 (i-1, i) 번째 */
    private static Map<String, Object> candidatesJson(int count, String difficulty, String category) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            candidates.add(new HashMap<>(Map.of(
                    "question", "질문 " + i + "번은 무엇일까요?",
                    "description", "설명 " + i,
                    "category", category,
                    "difficulty", difficulty,
                    "options", List.of(
                            new HashMap<>(Map.of("label", "찬성 " + i, "rationale", "이유 A" + i,
                                    "profile_id", PHILOSOPHERS.get((i - 1) % 10))),
                            new HashMap<>(Map.of("label", "반대 " + i, "rationale", "이유 B" + i,
                                    "profile_id", PHILOSOPHERS.get(i % 10)))))));
        }
        Map<String, Object> json = new HashMap<>();
        json.put("needs_clarification", false);
        json.put("candidates", candidates);
        return json;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstOption(Map<String, Object> json) {
        Map<String, Object> candidate = ((List<Map<String, Object>>) json.get("candidates")).get(0);
        return ((List<Map<String, Object>>) candidate.get("options")).get(0);
    }

    // ===== 헬퍼 =====

    private long createDraft() throws Exception {
        return data(mockMvc.perform(post(BASE).header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(draftBody("뫼르소처럼 장례식에서 울지 않은 사람을 비난할 수 있을까")))))
                .get("draftId").asLong();
    }

    private static Map<String, Object> draftBody(String seed) {
        return Map.of("topicSeed", seed, "difficulty", "INTERMEDIATE", "categories", List.of("철학", "문학", "철학"));
    }

    private ResultActions suggest(long draftId, Map<String, Object> body) throws Exception {
        var request = post(BASE + "/" + draftId + "/suggestions").header("Authorization", "Bearer " + ownerToken);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json(body));
        }
        return mockMvc.perform(request);
    }

    private ResultActions select(long draftId, int batchNo, int candidateId) throws Exception {
        return mockMvc.perform(post(BASE + "/" + draftId + "/selection").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("batchNo", batchNo, "candidateId", candidateId))));
    }

    private ResultActions validate(long draftId, long baseId, String question, String description,
                                   String labelA, String labelB, String philosopherA) throws Exception {
        Map<String, Object> optionA = new HashMap<>(Map.of("label", labelA, "rationale", "이유 A1"));
        if (philosopherA != null) {
            optionA.put("philosopher", philosopherA);
        }
        return mockMvc.perform(post(BASE + "/" + draftId + "/validations").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("baseVersionId", baseId, "question", question, "description", description,
                        "options", List.of(optionA, Map.of("label", labelB, "rationale", "이유 B1"))))));
    }

    private String candidateQuestion(long draftId, int index) throws Exception {
        return data(mockMvc.perform(get(BASE + "/" + draftId).header("Authorization", "Bearer " + ownerToken)))
                .get("candidates").get(index).get("question").asText();
    }

    private JsonNode data(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("data");
    }

    private AiJob onlyJob(AiJobType type) {
        List<AiJob> jobs = aiJobRepository.findAll().stream().filter(j -> j.getType() == type).toList();
        assertThat(jobs).hasSize(1);
        return jobs.get(0);
    }

    /** 분당 제한을 넘기기 위해 기존 작업의 시작 시각을 2분 전으로 돌린다 */
    private void ageJobs() {
        List<AiJob> jobs = aiJobRepository.findAll();
        jobs.forEach(j -> ReflectionTestUtils.setField(j, "startedAt", j.getStartedAt().minusMinutes(2)));
        aiJobRepository.saveAll(jobs);
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private User newUser(String nickname) {
        User user = userRepository.save(User.builder()
                .userTag("ct-" + UUID.randomUUID().toString().substring(0, 8))
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
