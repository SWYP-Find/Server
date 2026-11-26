package com.swyp.picke.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.classroom.entity.AiJob;
import com.swyp.picke.domain.classroom.entity.ClassDebateSession;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.ClassReport;
import com.swyp.picke.domain.classroom.entity.ClassReportView;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.AiJobType;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassReportType;
import com.swyp.picke.domain.oauth.entity.UserSocialAccount;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.util.TicketCodeHasher;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserBattle;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Figma '어드민 추가 페이지' 집계 API (#725). 테스트마다 롤백한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminClassUsageApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private EntityManager em;

    private String token;
    private String suffix;
    private User owner;
    private ClassTicket ticket;
    private Classroom classroom;
    private ClassMember memberA;
    private ClassMember memberB;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        token = jwtProvider.createAccessToken(newUser("admin-" + suffix, UserRole.ADMIN).getId(), "ADMIN");
    }

    @Test
    @DisplayName("이용권 목록은 계정 이메일·클래스 수·최근 활동·코드 확인 여부를 주고 이름순으로 정렬한다")
    void ticketList_hasAccountStatsAndNameSort() throws Exception {
        seedClass();
        ClassTicket unregistered = issueTicket("가나다 " + suffix);

        mockMvc.perform(get("/api/v1/admin/tickets").param("keyword", "owner-" + suffix)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].ticketId").value(ticket.getId()))
                .andExpect(jsonPath("$.data.content[0].maskedAccountEmail").value("o***@example.com"))
                .andExpect(jsonPath("$.data.content[0].classCount").value(1))
                .andExpect(jsonPath("$.data.content[0].codeAvailable").value(false))
                .andExpect(jsonPath("$.data.content[0].lastActiveAt").isNotEmpty());

        // 등록 계정 닉네임(owner-…)과 구매자 이름(가나다 …)을 섞어 이름순으로 정렬한다
        JsonNode asc = data(get("/api/v1/admin/tickets").param("keyword", suffix).param("sort", "NAME_ASC"));
        JsonNode desc = data(get("/api/v1/admin/tickets").param("keyword", suffix).param("sort", "NAME_DESC"));
        assertThat(asc.get("content").get(0).get("ticketId").asLong()).isEqualTo(ticket.getId());
        assertThat(desc.get("content").get(0).get("ticketId").asLong()).isEqualTo(unregistered.getId());
        assertThat(asc.get("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    @DisplayName("이용권 계정 이용 현황은 클래스·참가자·배틀 완료·최근 리포트와 클래스별 활동을 센다")
    void ticketUsage_countsClassActivity() throws Exception {
        seedClass();
        mockMvc.perform(get("/api/v1/admin/tickets/{ticketId}/usage", ticket.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classCount").value(1))
                .andExpect(jsonPath("$.data.participantCount").value(3))
                .andExpect(jsonPath("$.data.battleCompletedCount").value(1))
                .andExpect(jsonPath("$.data.reportsLast30Days").value(1))
                .andExpect(jsonPath("$.data.classes[0].classId").value(classroom.getId()))
                .andExpect(jsonPath("$.data.classes[0].debateCount").value(1))
                .andExpect(jsonPath("$.data.classes[0].reportCount").value(1));

        mockMvc.perform(get("/api/v1/admin/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registeredEmail").value("owner-" + suffix + "@example.com"))
                .andExpect(jsonPath("$.data.registeredUserTag").value("owner-" + suffix));

        mockMvc.perform(get("/api/v1/admin/tickets/{ticketId}/usage", 987654321L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("클래스 성과는 배틀 시작·리포트 완료율·참가자별 댓글 수와 리포트 상태, AI 오류를 준다")
    void classPerformance_listsParticipants() throws Exception {
        seedClass();
        JsonNode data = data(get("/api/v1/admin/classes/{classId}/performance", classroom.getId()).param("size", "1"));
        // 운영자는 참여하지 않는 클래스라 리포트 대상은 멤버 2명
        assertThat(data.get("reportTargetCount").asLong()).isEqualTo(2);
        assertThat(data.get("battleStartedCount").asLong()).isEqualTo(2);
        assertThat(data.get("reportCompletedCount").asLong()).isEqualTo(1);
        assertThat(data.get("reportCompletionRate").asDouble()).isEqualTo(50.0);
        assertThat(data.get("ownerEmail").asText()).isEqualTo("owner-" + suffix + "@example.com");
        assertThat(data.get("aiErrorCount").asLong()).isEqualTo(1);
        assertThat(data.get("aiErrors").get(0).get("errorCode").asText()).isEqualTo("TIMEOUT");
        JsonNode participants = data.get("participants");
        assertThat(participants.get("totalElements").asLong()).isEqualTo(2);
        assertThat(participants.get("content")).hasSize(1);
        JsonNode first = participants.get("content").get(0);
        assertThat(first.get("memberId").asLong()).isEqualTo(memberA.getId());
        assertThat(first.get("commentCount").asLong()).isEqualTo(2);
        assertThat(first.get("reportStatus").asText()).isEqualTo("SUCCEEDED");
        assertThat(first.get("battleStep").asText()).isEqualTo("COMPLETED");

        JsonNode second = data(get("/api/v1/admin/classes/{classId}/performance", classroom.getId())
                .param("page", "1").param("size", "1")).get("participants").get("content").get(0);
        assertThat(second.get("memberId").asLong()).isEqualTo(memberB.getId());
        assertThat(second.get("reportStatus").isNull()).isTrue();
    }

    @Test
    @DisplayName("성과 분석은 이용권 누적 현황과 기간 안 사용 흐름·일별 리포트·AI 오류를 센다")
    void analytics_countsOperationAndFlow() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        JsonNode before = data(get("/api/v1/admin/classes/analytics"));
        seedClass();
        issueTicket("미등록 " + suffix);
        JsonNode after = data(get("/api/v1/admin/classes/analytics")
                .param("from", today.minusDays(29).toString()).param("to", today.toString()));

        assertThat(delta(before, after, "operation", "issuedTotal")).isEqualTo(2);
        assertThat(delta(before, after, "operation", "registered")).isEqualTo(1);
        assertThat(delta(before, after, "operation", "activeAccounts")).isEqualTo(1);
        assertThat(delta(before, after, "flow", "operatorsWithReport")).isEqualTo(1);
        assertThat(delta(before, after, "flow", "battlesStarted")).isEqualTo(2);
        assertThat(delta(before, after, "flow", "battlesWithReport")).isEqualTo(1);
        assertThat(delta(before, after, "flow", "reportsCompleted")).isEqualTo(1);
        assertThat(delta(before, after, "flow", "reportsViewed")).isEqualTo(1);
        assertThat(after.get("dailyReports")).hasSize(30);
        assertThat(after.get("aiErrorTotal").asLong() - before.get("aiErrorTotal").asLong()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/admin/classes/analytics")
                        .param("from", today.toString()).param("to", today.minusDays(1).toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    private void seedClass() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        owner = newUser("owner-" + suffix, UserRole.USER);
        em.persist(UserSocialAccount.builder().user(owner).provider("GOOGLE")
                .providerUserId("g-" + suffix).providerEmail("owner-" + suffix + "@example.com").build());
        ticket = issueTicket("후원자 " + suffix);
        ticket.register(owner, now.minusDays(1));

        Battle battle = Battle.builder().title("주제 " + suffix).summary("요약")
                .status(BattleStatus.PUBLISHED).creatorType(BattleCreatorType.ADMIN).build();
        em.persist(battle);
        BattleOption optionA = BattleOption.builder().battle(battle).title("A").stance("찬성").displayOrder(1).build();
        BattleOption optionB = BattleOption.builder().battle(battle).title("B").stance("반대").displayOrder(2).build();
        em.persist(optionA);
        em.persist(optionB);

        classroom = Classroom.builder().name("클래스 " + suffix)
                .inviteCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .owner(owner).ticket(ticket).battle(battle).battleType(ClassBattleType.TTS)
                .capacity(5).deadline(now.plusDays(5)).commentRequired(false).ownerParticipates(false).build();
        em.persist(classroom);
        em.persist(ClassMember.builder().classroom(classroom).user(owner).displayName("운영자")
                .role(ClassMemberRole.OWNER).joinedAt(now).build());
        User userA = newUser("member-a-" + suffix, UserRole.USER);
        User userB = newUser("member-b-" + suffix, UserRole.USER);
        memberA = ClassMember.builder().classroom(classroom).user(userA).displayName("가")
                .role(ClassMemberRole.MEMBER).joinedAt(now).build();
        memberB = ClassMember.builder().classroom(classroom).user(userB).displayName("나")
                .role(ClassMemberRole.MEMBER).joinedAt(now.plusSeconds(1)).build();
        em.persist(memberA);
        em.persist(memberB);

        em.persist(UserBattle.builder().user(userA).battle(battle).classroom(classroom).step(UserBattleStep.COMPLETED).build());
        em.persist(UserBattle.builder().user(userB).battle(battle).classroom(classroom).step(UserBattleStep.PRE_VOTE).build());

        Perspective perspective = Perspective.builder().battle(battle).user(userA).option(optionA)
                .classroom(classroom).content("의견입니다 " + suffix).build();
        perspective.publish();
        em.persist(perspective);
        em.persist(PerspectiveComment.builder().perspective(perspective).user(userA).classroom(classroom)
                .content("댓글입니다").build());

        ClassReport report = ClassReport.builder().classroom(classroom).member(memberA)
                .type(ClassReportType.MEMBER_FEEDBACK).build();
        report.complete(Map.of("title", "리포트"), now);
        em.persist(report);
        em.persist(new ClassReportView(classroom.getId(), report.getId(), userA.getId(), now));

        em.persist(ClassDebateSession.builder().classroom(classroom).member(memberA).participantOption(optionA)
                .opponentOption(optionB).philosopher(PhilosopherType.SOCRATES).now(now).build());
        AiJob failed = AiJob.builder().type(AiJobType.MEMBER_REPORT).idempotencyKey("fail-" + suffix)
                .ticket(ticket).classroom(classroom).member(memberB).build();
        failed.fail("TIMEOUT", now);
        em.persist(failed);
        em.flush();
    }

    private ClassTicket issueTicket(String supporterName) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        FundingOrder order = FundingOrder.builder().platform(FundingPlatform.MANUAL).campaignId("campaign-" + suffix)
                .orderId("order-" + UUID.randomUUID()).rewardTier(RewardTier.SEAT_5).quantity(1)
                .supporterName(supporterName).paymentStatus(PaymentStatus.PAID_CONFIRMED).paidAt(now.minusDays(2)).build();
        em.persist(order);
        ClassTicket issued = ClassTicket.issue(order, TicketCodeHasher.hash(UUID.randomUUID().toString()));
        em.persist(issued);
        em.flush();
        return issued;
    }

    private User newUser(String tag, UserRole role) {
        User user = User.builder().userTag(tag).nickname(tag).role(role).status(UserStatus.ACTIVE).build();
        em.persist(user);
        return user;
    }

    private JsonNode data(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        em.flush();
        String body = mockMvc.perform(request.header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data");
    }

    private static long delta(JsonNode before, JsonNode after, String group, String field) {
        return after.get(group).get(field).asLong() - before.get(group).get(field).asLong();
    }
}
