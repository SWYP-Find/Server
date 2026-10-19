package com.swyp.picke.domain.classroom.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.entity.AiJob;
import com.swyp.picke.domain.classroom.entity.ClassReport;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.AiJobStatus;
import com.swyp.picke.domain.classroom.enums.AiJobType;
import com.swyp.picke.domain.classroom.enums.ClassReportStatus;
import com.swyp.picke.domain.classroom.enums.ClassReportType;
import com.swyp.picke.domain.classroom.repository.AiJobRepository;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassReportRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.service.report.ClassReportAiClient;
import com.swyp.picke.domain.classroom.service.report.ClassReportGenerationService;
import com.swyp.picke.domain.classroom.service.report.ClassReportPrompts;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 클래스 결과·AI 리포트(PRD ⑤)를 HTTP + 생성 파이프라인으로 검증한다. AI 제공자는 모킹하고 프롬프트에 실린 source_id 를 그대로 돌려준다.
 * 기준: PRD §12(개인 입력), §13(출력·근거 검증·자료 부족 템플릿), §14(인사이트·3명 미만), §15(마감 전 문구, 30일 열람), §21(비용 기록), §22(실패·재시도).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassResultApiIntegrationTest {

    private static final Pattern STATEMENT = Pattern.compile("source_id=(p:\\d+)[^\\n]*\\n(?:  \\(부모[^\\n]*\\n)?  원문: ([^\\n]+)");
    private static final Pattern ANY_SOURCE = Pattern.compile("source_id=([pc]:\\d+)");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private FundingOrderRepository fundingOrderRepository;
    @Autowired private ClassTicketRepository classTicketRepository;
    @Autowired private BattleRepository battleRepository;
    @Autowired private BattleOptionRepository battleOptionRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ClassMemberRepository classMemberRepository;
    @Autowired private BattleVoteRepository battleVoteRepository;
    @Autowired private UserBattleRepository userBattleRepository;
    @Autowired private PerspectiveRepository perspectiveRepository;
    @Autowired private PerspectiveCommentRepository perspectiveCommentRepository;
    @Autowired private ClassReportRepository classReportRepository;
    @Autowired private AiJobRepository aiJobRepository;
    @Autowired private ClassReportGenerationService generationService;

    @MockitoBean
    private ClassReportAiClient aiClient;

    private final List<Long> createdBattleIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private User owner;
    private String ownerToken;
    private Battle battle;
    private BattleOption optionA;
    private BattleOption optionB;

    @BeforeEach
    void setUp() {
        cleanUp();
        owner = newUser("선생님");
        ownerToken = tokenOf(owner);
        issueAndRegister(owner);
        battle = newBattle();
        optionA = newOption(battle, 1);
        optionB = newOption(battle, 2);
        when(aiClient.getModel()).thenReturn("test-model");
        stubMemberFeedback();
        stubInsightSuccess();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        for (Long battleId : createdBattleIds) {
            battleRepository.findById(battleId).ifPresent(b -> {
                battleOptionRepository.deleteAll(battleOptionRepository.findByBattle(b));
                battleRepository.delete(b);
            });
        }
        createdBattleIds.clear();
        for (Long userId : createdUserIds) {
            userProfileRepository.findByUserId(userId).ifPresent(userProfileRepository::delete);
        }
        createdUserIds.clear();
    }

    private void cleanUp() {
        classReportRepository.deleteAll();
        aiJobRepository.deleteAll();
        perspectiveCommentRepository.deleteAll();
        perspectiveRepository.deleteAll();
        battleVoteRepository.deleteAll();
        userBattleRepository.deleteAll();
        classMemberRepository.deleteAll();
        classroomRepository.deleteAll();
        classTicketRepository.deleteAll();
        fundingOrderRepository.deleteAll();
    }

    @Test
    @DisplayName("마감 전: 통계는 LIVE, 내 리포트는 BEFORE_CLOSE, 인사이트는 운영자만(BEFORE_CLOSE), AI 호출 없음")
    void beforeClose() throws Exception {
        ClassInfo cls = createClass(false);
        User student = newUser("학생A");
        String token = tokenOf(student);
        join(token, cls.inviteCode, "학생A");
        completeParticipation(token, cls, optionA, optionB, "효과가 입증되지 않아 반대합니다. 표본이 너무 작아요.");

        mockMvc.perform(get(result(cls)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closed").value(false))
                .andExpect(jsonPath("$.data.statsBasis").value("LIVE"))
                .andExpect(jsonPath("$.data.stats.activeMemberCount").value(1))
                .andExpect(jsonPath("$.data.stats.completedCount").value(1))
                .andExpect(jsonPath("$.data.stats.participationRate").value(100.0))
                .andExpect(jsonPath("$.data.stats.commentRate").value(100.0))
                .andExpect(jsonPath("$.data.stats.options[1].postVoteCount").value(1))
                .andExpect(jsonPath("$.data.stats.options[1].postVoteRatio").value(100.0))
                .andExpect(jsonPath("$.data.myReportState").value("BEFORE_CLOSE"))
                .andExpect(jsonPath("$.data.insightState").doesNotExist());

        mockMvc.perform(get(result(cls) + "/my-report").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("BEFORE_CLOSE"))
                .andExpect(jsonPath("$.data.closeAt").exists())
                .andExpect(jsonPath("$.data.preOption.optionId").value(optionA.getId()))
                .andExpect(jsonPath("$.data.postOption.optionId").value(optionB.getId()))
                .andExpect(jsonPath("$.data.perspectiveCount").value(1))
                .andExpect(jsonPath("$.data.feedback").doesNotExist());

        mockMvc.perform(get(result(cls) + "/insight").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_OWNER"));
        mockMvc.perform(get(result(cls) + "/insight").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("BEFORE_CLOSE"))
                .andExpect(jsonPath("$.data.retryable").value(false));

        // 참여하지 않는 운영자는 개인 리포트가 없다 / 멤버 아닌 사람은 결과를 못 본다
        mockMvc.perform(get(result(cls) + "/my-report").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));
        mockMvc.perform(get(result(cls)).header("Authorization", "Bearer " + tokenOf(newUser("외부인"))))
                .andExpect(status().isForbidden());

        // 마감 전에는 스케줄러가 돌아도 아무것도 만들지 않는다
        assertThat(generationService.runOnce(LocalDateTime.now())).isZero();
        assertThat(classReportRepository.count()).isZero();
        verify(aiClient, never()).chatJson(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("마감 후 1회 생성: 완료자 AI 피드백(근거 검증), 투표만 한 사람 LIMITED 템플릿, 미완료자 NOT_ELIGIBLE, 통계 SNAPSHOT, 비용 기록")
    void afterClose_generatesReports() throws Exception {
        ClassInfo cls = createClass(false);
        User writer = newUser("학생A");
        String writerToken = tokenOf(writer);
        join(writerToken, cls.inviteCode, "학생A");
        completeParticipation(writerToken, cls, optionA, optionB, "효과가 입증되지 않아 반대합니다. 표본이 너무 작아요.");

        User voteOnly = newUser("학생B");           // 의견 없이 투표만 → comment_required=false 클래스가 아니므로 참여 미완료
        String voteOnlyToken = tokenOf(voteOnly);
        join(voteOnlyToken, cls.inviteCode, "학생B");
        votePreAndPost(voteOnlyToken, cls, optionA, optionA);

        User idle = newUser("학생C");               // 아무것도 안 함
        String idleToken = tokenOf(idle);
        join(idleToken, cls.inviteCode, "학생C");

        closeClass(cls.classId);
        LocalDateTime now = LocalDateTime.now();
        assertThat(generationService.runOnce(now)).isEqualTo(2); // CLASS_SUMMARY + 완료자 1명

        // 통계는 마감 스냅샷 (이후 멤버가 바뀌어도 고정)
        mockMvc.perform(get(result(cls)).header("Authorization", "Bearer " + writerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closed").value(true))
                .andExpect(jsonPath("$.data.statsBasis").value("SNAPSHOT"))
                .andExpect(jsonPath("$.data.cutoffAt").exists())
                .andExpect(jsonPath("$.data.stats.activeMemberCount").value(3))
                .andExpect(jsonPath("$.data.stats.completedCount").value(1))
                .andExpect(jsonPath("$.data.stats.participationRate").value(33.3))
                .andExpect(jsonPath("$.data.stats.postVoteCount").value(2))
                .andExpect(jsonPath("$.data.stats.options[0].postVoteRatio").value(50.0))
                .andExpect(jsonPath("$.data.myReportState").value("SUCCEEDED"));

        // 완료자: AI 피드백 + 근거는 본인 의견 원문으로 다시 조회
        mockMvc.perform(get(result(cls) + "/my-report").header("Authorization", "Bearer " + writerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.feedback.headline").value("근거를 요구하는 판단"))
                .andExpect(jsonPath("$.data.feedback.strength.title").value("근거 점검"))
                .andExpect(jsonPath("$.data.feedback.nextFocus").doesNotExist())
                .andExpect(jsonPath("$.data.feedback.analysisStatus").value("SUFFICIENT"))
                .andExpect(jsonPath("$.data.feedback.coverage").value("full"))
                .andExpect(jsonPath("$.data.feedback.evidence", hasSize(2)))
                .andExpect(jsonPath("$.data.feedback.evidence[0].sourceType").value("PERSPECTIVE"))
                .andExpect(jsonPath("$.data.feedback.evidence[0].content").value("효과가 입증되지 않아 반대합니다. 표본이 너무 작아요."))
                .andExpect(jsonPath("$.data.generatedAt").exists());

        // 참여 미완료자는 리포트 행 자체가 없다
        mockMvc.perform(get(result(cls) + "/my-report").header("Authorization", "Bearer " + voteOnlyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.data.postOption.optionId").value(optionA.getId()));
        mockMvc.perform(get(result(cls) + "/my-report").header("Authorization", "Bearer " + idleToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("NOT_ELIGIBLE"));

        // 운영자 인사이트: 공개 글 작성자 1명 → 3명 미만 템플릿, AI 호출 없음
        mockMvc.perform(get(result(cls) + "/insight").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("LIMITED"))
                .andExpect(jsonPath("$.data.insightText").value("아직 요약할 의견이 충분하지 않아요"))
                .andExpect(jsonPath("$.data.evidence", hasSize(0)))
                .andExpect(jsonPath("$.data.highlights", hasSize(1)));

        // 비용 기록: 개인 1건만 (인사이트는 템플릿이라 호출 없음), 재실행해도 다시 만들지 않는다
        verify(aiClient, times(1)).chatJson(eq(ClassReportPrompts.MEMBER_FEEDBACK_SYSTEM), anyString(), anyInt());
        verify(aiClient, never()).chatJson(eq(ClassReportPrompts.CLASS_INSIGHT_SYSTEM), anyString(), anyInt());
        List<AiJob> jobs = aiJobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getType()).isEqualTo(AiJobType.MEMBER_REPORT);
        assertThat(jobs.get(0).getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
        assertThat(jobs.get(0).getInputTokens()).isEqualTo(120);
        assertThat(jobs.get(0).getTicket()).isNotNull();
        assertThat(generationService.runOnce(now.plusMinutes(1))).isZero();
        assertThat(classReportRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("의견 선택 클래스에서 투표만 한 완료자는 AI 없이 서버 템플릿(LIMITED)을 받는다")
    void voteOnlyMember_getsTemplate() throws Exception {
        ClassInfo cls = createClass(false, false);
        User student = newUser("학생A");
        String token = tokenOf(student);
        join(token, cls.inviteCode, "학생A");
        votePreAndPost(token, cls, optionA, optionB);

        closeClass(cls.classId);
        generationService.runOnce(LocalDateTime.now());

        mockMvc.perform(get(result(cls) + "/my-report").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("LIMITED"))
                .andExpect(jsonPath("$.data.feedback.headline").value("선택의 이유를 확인할 글이 아직 없어요"))
                .andExpect(jsonPath("$.data.feedback.judgmentBasis").value("선택이 선택지 1에서 선택지 2로 바뀌었어요."))
                .andExpect(jsonPath("$.data.feedback.nextQuestion").value("지금의 선택을 한 가장 중요한 이유는 무엇인가요?"))
                .andExpect(jsonPath("$.data.feedback.strength").doesNotExist())
                .andExpect(jsonPath("$.data.feedback.nextFocus").doesNotExist())
                .andExpect(jsonPath("$.data.feedback.evidence", hasSize(0)));
        verify(aiClient, never()).chatJson(anyString(), anyString(), anyInt());
        assertThat(aiJobRepository.count()).isZero();
    }

    @Test
    @DisplayName("인사이트: 작성자 3명 이상이면 AI 요약 + 근거·하이라이트, 실패 시 자동 2회 → FAILED → 운영자 재시도 → 성공")
    void insight_retryFlow() throws Exception {
        ClassInfo cls = createClass(false);
        String[] contents = {
                "효과가 입증되지 않아 반대합니다. 표본이 너무 작아요.",
                "비용보다 기대 효과가 크다고 보아 찬성합니다. 장기적으로 이득이에요.",
                "조건부로 찬성합니다. 검증 기간을 두면 괜찮을 것 같아요."
        };
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            User student = newUser("학생" + i);
            String token = tokenOf(student);
            join(token, cls.inviteCode, "학생" + i);
            completeParticipation(token, cls, optionA, i == 0 ? optionB : optionA, contents[i]);
            tokens.add(token);
        }
        // 댓글 1건 (공개 글에 포함, 작성자는 이미 3명)
        Perspective first = perspectiveRepository.findAll().get(0);
        Classroom classroom = classroomRepository.findById(cls.classId).orElseThrow();
        perspectiveCommentRepository.save(PerspectiveComment.builder()
                .perspective(first).user(first.getUser()).classroom(classroom).content("보충하자면 표본은 30명이었어요.").build());

        // 인사이트 호출은 두 번 연속 실패 → 자동 재시도 소진 → FAILED
        when(aiClient.chatJson(eq(ClassReportPrompts.CLASS_INSIGHT_SYSTEM), anyString(), anyInt()))
                .thenThrow(new RuntimeException("provider down"));
        closeClass(cls.classId);
        LocalDateTime now = LocalDateTime.now();
        generationService.runOnce(now);               // 1차 시도 실패 → PENDING 복귀
        ClassReport summary = summaryOf(cls.classId);
        assertThat(summary.getStatus()).isEqualTo(ClassReportStatus.PENDING);
        assertThat(jobOf(summary).getAttempt()).isEqualTo(1);
        generationService.runOnce(now.plusMinutes(1)); // 2차 시도 실패 → FAILED
        summary = summaryOf(cls.classId);
        assertThat(summary.getStatus()).isEqualTo(ClassReportStatus.FAILED);
        assertThat(jobOf(summary).getStatus()).isEqualTo(AiJobStatus.FAILED);
        assertThat(jobOf(summary).getAttempt()).isEqualTo(2);

        mockMvc.perform(get(result(cls) + "/insight").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("FAILED"))
                .andExpect(jsonPath("$.data.retryable").value(true))
                .andExpect(jsonPath("$.data.insightText").value("AI 요약을 준비하지 못했어요"));
        // 통계는 실패와 무관하게 제공, 개인 리포트도 각자 성공
        mockMvc.perform(get(result(cls)).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statsBasis").value("SNAPSHOT"))
                .andExpect(jsonPath("$.data.stats.completedCount").value(3))
                .andExpect(jsonPath("$.data.insightState").value("FAILED"))
                .andExpect(jsonPath("$.data.myReportState").doesNotExist());
        mockMvc.perform(get(result(cls) + "/my-report").header("Authorization", "Bearer " + tokens.get(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("SUCCEEDED"));

        // 멤버는 재시도 불가, 운영자 재시도 → PENDING → 다음 틱에 성공
        mockMvc.perform(post(result(cls) + "/insight/retry").header("Authorization", "Bearer " + tokens.get(0)))
                .andExpect(status().isForbidden());
        stubInsightSuccess();
        mockMvc.perform(post(result(cls) + "/insight/retry").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PENDING"));
        mockMvc.perform(post(result(cls) + "/insight/retry").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_REPORT_RETRY"));
        generationService.runOnce(now.plusMinutes(2));

        mockMvc.perform(get(result(cls) + "/insight").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.retryable").value(false))
                .andExpect(jsonPath("$.data.insightText").value("찬성 쪽은 비용 대비 효과를, 반대 쪽은 표본 크기를 기준으로 삼은 의견이 눈에 띄었어요."))
                .andExpect(jsonPath("$.data.coverage").value("full"))
                .andExpect(jsonPath("$.data.evidence", hasSize(2)))
                .andExpect(jsonPath("$.data.evidence[0].content").exists())
                .andExpect(jsonPath("$.data.highlights", hasSize(2)))
                .andExpect(jsonPath("$.data.generatedAt").exists());
        summary = summaryOf(cls.classId);
        assertThat(jobOf(summary).getAttempt()).isEqualTo(3);
        assertThat(jobOf(summary).getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
        // 같은 ai_job 1행에 시도가 누적되고, 개인 3건 + 인사이트 1건 = 4행
        assertThat(aiJobRepository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("AI 응답이 근거 검증을 통과하지 못하면 REJECTED 로 기록하고 재시도한다")
    void invalidEvidence_isRejectedAndRetried() throws Exception {
        ClassInfo cls = createClass(false);
        User student = newUser("학생A");
        String token = tokenOf(student);
        join(token, cls.inviteCode, "학생A");
        completeParticipation(token, cls, optionA, optionB, "효과가 입증되지 않아 반대합니다. 표본이 너무 작아요.");

        // 1차: 존재하지 않는 발언을 인용 → 거절, 2차: 정상
        when(aiClient.chatJson(eq(ClassReportPrompts.MEMBER_FEEDBACK_SYSTEM), anyString(), anyInt()))
                .thenReturn(new ClassReportAiClient.AiChatResult(Map.of(
                        "headline", "h", "judgment_basis", "j", "next_question", "q",
                        "evidence_refs", List.of(Map.of("field", "judgment_basis", "source_id", "p:999999", "quote", "없는 문장"))
                ), "{}", 10, 5))
                .thenAnswer(inv -> memberFeedbackAnswer(inv.getArgument(1)));

        closeClass(cls.classId);
        LocalDateTime now = LocalDateTime.now();
        generationService.runOnce(now);
        ClassReport report = classReportRepository.findAll().stream()
                .filter(r -> r.getType() == ClassReportType.MEMBER_FEEDBACK).findFirst().orElseThrow();
        assertThat(report.getStatus()).isEqualTo(ClassReportStatus.PENDING);
        assertThat(jobOf(report).getStatus()).isEqualTo(AiJobStatus.REJECTED);
        assertThat(jobOf(report).getErrorCode()).isEqualTo("EVIDENCE_MISSING");

        mockMvc.perform(get(result(cls) + "/my-report").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PENDING"))
                .andExpect(jsonPath("$.data.feedback").doesNotExist());

        generationService.runOnce(now.plusMinutes(1));
        mockMvc.perform(get(result(cls) + "/my-report").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("SUCCEEDED"));
    }

    @Test
    @DisplayName("이용권 종료 + 30일이 지나면 결과·리포트 조회가 409 CLASS_409_RESULT_EXPIRED")
    void readGraceExpired() throws Exception {
        ClassInfo cls = createClass(true);
        ClassTicket ticket = classTicketRepository.findAll().get(0);
        ReflectionTestUtils.setField(ticket, "expiresAt", LocalDateTime.now().minusDays(31));
        classTicketRepository.save(ticket);

        for (String path : new String[]{"", "/my-report", "/insight"}) {
            mockMvc.perform(get(result(cls) + path).header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("CLASS_409_RESULT_EXPIRED"));
        }
        mockMvc.perform(post(result(cls) + "/insight/retry").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_RESULT_EXPIRED"));
    }

    // ===== AI 모킹 =====

    /** 프롬프트의 첫 발언(source_id, 원문)을 그대로 인용하는 정상 응답. next_focus 는 원문에 없는 인용이라 null 이 돼야 한다 */
    private void stubMemberFeedback() {
        when(aiClient.chatJson(eq(ClassReportPrompts.MEMBER_FEEDBACK_SYSTEM), anyString(), anyInt()))
                .thenAnswer(inv -> memberFeedbackAnswer(inv.getArgument(1)));
    }

    private static ClassReportAiClient.AiChatResult memberFeedbackAnswer(String userPrompt) {
        Matcher m = STATEMENT.matcher(userPrompt);
        if (!m.find()) {
            throw new IllegalStateException("no statement in prompt:\n" + userPrompt);
        }
        String sourceId = m.group(1);
        String quote = m.group(2).substring(0, 8);
        Map<String, Object> json = new HashMap<>();
        json.put("headline", "근거를 요구하는 판단");
        json.put("judgment_basis", "효과의 근거를 먼저 요구하는 기준으로 판단했어요.");
        json.put("strength", Map.of("title", "근거 점검", "body", "표본 크기를 짚으며 근거의 한계를 확인했어요."));
        json.put("next_focus", Map.of("title", "조건 구체화", "body", "어떤 조건이면 바뀔지 적어 보세요."));
        json.put("next_question", "어떤 데이터가 있으면 판단이 바뀔까요?");
        json.put("evidence_refs", List.of(
                Map.of("field", "judgment_basis", "source_id", sourceId, "quote", quote),
                Map.of("field", "strength", "source_id", sourceId, "quote", quote),
                Map.of("field", "next_focus", "source_id", sourceId, "quote", "원문에 없는 인용")
        ));
        json.put("analysis_status", "SUFFICIENT");
        return new ClassReportAiClient.AiChatResult(json, "{}", 120, 80);
    }

    /** 프롬프트에 실린 공개 글 중 앞의 2건을 근거로, 1건을 하이라이트로 돌려주는 정상 응답 */
    private void stubInsightSuccess() {
        when(aiClient.chatJson(eq(ClassReportPrompts.CLASS_INSIGHT_SYSTEM), anyString(), anyInt()))
                .thenAnswer(inv -> {
                    Matcher m = ANY_SOURCE.matcher(inv.getArgument(1));
                    List<String> ids = new ArrayList<>();
                    while (m.find() && ids.size() < 2) {
                        ids.add(m.group(1));
                    }
                    Map<String, Object> json = Map.of(
                            "insight_text", "찬성 쪽은 비용 대비 효과를, 반대 쪽은 표본 크기를 기준으로 삼은 의견이 눈에 띄었어요.",
                            "evidence_ids", ids,
                            "highlight_ids", List.of(ids.get(0)));
                    return new ClassReportAiClient.AiChatResult(json, "{}", 300, 90);
                });
    }

    // ===== 헬퍼 =====

    private record ClassInfo(long classId, String inviteCode) {
    }

    private static String result(ClassInfo cls) {
        return "/api/v1/classes/" + cls.classId + "/result";
    }

    private static String classBattle(ClassInfo cls) {
        return "/api/v1/classes/" + cls.classId + "/battle";
    }

    /** 지연 로딩 프록시를 세션 밖에서 읽지 않도록 ai_jobs 를 다시 조회 */
    private AiJob jobOf(ClassReport report) {
        return aiJobRepository.findById(report.getAiJob().getId()).orElseThrow();
    }

    private ClassReport summaryOf(long classId) {
        return classReportRepository.findByClassroomIdAndTypeAndMemberIsNull(classId, ClassReportType.CLASS_SUMMARY).orElseThrow();
    }

    private void votePreAndPost(String token, ClassInfo cls, BattleOption pre, BattleOption post) throws Exception {
        vote(token, cls, "/votes/pre", pre).andExpect(status().isOk());
        mockMvc.perform(post(classBattle(cls) + "/votes/tts-complete").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        vote(token, cls, "/votes/post", post).andExpect(status().isOk());
    }

    private void completeParticipation(String token, ClassInfo cls, BattleOption pre, BattleOption post, String content) throws Exception {
        votePreAndPost(token, cls, pre, post);
        mockMvc.perform(post(classBattle(cls) + "/perspectives")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", content))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    private ResultActions vote(String token, ClassInfo cls, String path, BattleOption option) throws Exception {
        return mockMvc.perform(post(classBattle(cls) + path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("optionId", option.getId()))));
    }

    private void closeClass(long classId) {
        Classroom classroom = classroomRepository.findById(classId).orElseThrow();
        ReflectionTestUtils.setField(classroom, "deadline", LocalDateTime.now().minusMinutes(1));
        classroomRepository.save(classroom);
    }

    private ClassInfo createClass(boolean ownerParticipates) throws Exception {
        return createClass(ownerParticipates, true);
    }

    private ClassInfo createClass(boolean ownerParticipates, boolean commentRequired) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "클래스");
        body.put("battleId", battle.getId());
        body.put("deadline", null);
        body.put("commentRequired", commentRequired);
        body.put("ownerParticipates", ownerParticipates);
        MvcResult result = mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return new ClassInfo(data.get("classId").asLong(), data.get("inviteCode").asText());
    }

    private void join(String token, String code, String name) throws Exception {
        mockMvc.perform(post("/api/v1/classes/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("inviteCode", code, "displayName", name))))
                .andExpect(status().isOk());
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private User newUser(String nickname) {
        User user = userRepository.save(User.builder()
                .userTag("cr-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        userProfileRepository.save(UserProfile.builder()
                .user(user).nickname(nickname).characterType(CharacterType.OWL)
                .mannerTemperature(BigDecimal.valueOf(36.5))
                .build());
        createdUserIds.add(user.getId());
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

    private Battle newBattle() {
        Battle saved = battleRepository.save(Battle.builder()
                .title("클래스 배틀 " + UUID.randomUUID().toString().substring(0, 4))
                .summary("요약")
                .description("설명")
                .thumbnailUrl("https://img/class.png")
                .targetDate(LocalDate.now())
                .status(BattleStatus.PUBLISHED)
                .creatorType(BattleCreatorType.ADMIN)
                .build());
        createdBattleIds.add(saved.getId());
        return saved;
    }

    private BattleOption newOption(Battle battle, int order) {
        return battleOptionRepository.save(BattleOption.builder()
                .battle(battle)
                .title("선택지 " + order)
                .stance(order == 1 ? "찬성" : "반대")
                .displayOrder(order)
                .build());
    }
}
