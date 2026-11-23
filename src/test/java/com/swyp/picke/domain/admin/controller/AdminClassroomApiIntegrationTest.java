package com.swyp.picke.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.repository.FundingOrderRepository;
import com.swyp.picke.domain.ticket.util.TicketCodeHasher;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminClassroomApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private FundingOrderRepository fundingOrderRepository;
    @Autowired private ClassTicketRepository classTicketRepository;
    @Autowired private BattleRepository battleRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ClassMemberRepository classMemberRepository;

    private final List<Long> classIds = new ArrayList<>();
    private final List<Long> ticketIds = new ArrayList<>();
    private final List<Long> orderIds = new ArrayList<>();
    private final List<Long> battleIds = new ArrayList<>();
    private String token;
    private Long openClassId;
    private Long closedClassId;
    private String openClassName;
    private String closedClassName;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        token = jwtProvider.createAccessToken(newUser("admin-" + suffix, UserRole.ADMIN).getId(), "ADMIN");
        openClassName = "열린 클래스 " + suffix;
        closedClassName = "마감된 클래스 " + suffix;
        openClassId = createClass(suffix, openClassName, LocalDateTime.now().plusDays(10));
        closedClassId = createClass(suffix, closedClassName, LocalDateTime.now().minusDays(1));
    }

    @AfterEach
    void tearDown() {
        for (Long classId : classIds) {
            classMemberRepository.deleteAll(classMemberRepository.findActiveMembers(classId));
        }
        classroomRepository.deleteAllById(classIds);
        classTicketRepository.deleteAllById(ticketIds);
        fundingOrderRepository.deleteAllById(orderIds);
        battleRepository.deleteAllById(battleIds);
        classIds.clear();
        ticketIds.clear();
        orderIds.clear();
        battleIds.clear();
    }

    @Test
    @DisplayName("관리자 클래스 목록은 키워드·상태·정원·배틀 유형을 필터링하고 상세에서 마감 상태를 구분한다")
    void listAndDetail_areReadOnlyAndReportClosedState() throws Exception {
        mockMvc.perform(get("/api/v1/admin/classes")
                        .param("keyword", openClassName)
                        .param("state", "OPEN")
                        .param("capacity", "5")
                        .param("battleType", "TTS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].classId").value(openClassId))
                .andExpect(jsonPath("$.data.content[0].state").value("OPEN"));

        mockMvc.perform(get("/api/v1/admin/classes")
                        .param("state", "CLOSED")
                        .param("keyword", closedClassName)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].classId").value(closedClassId));

        mockMvc.perform(get("/api/v1/admin/classes/{classId}", closedClassId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("CLOSED"))
                .andExpect(jsonPath("$.data.ticket.codeSuffix").value("1234"))
                .andExpect(jsonPath("$.data.progress.activeParticipants").value(1));

        mockMvc.perform(get("/api/v1/admin/classes/{classId}", 987654321)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("관리자 클래스 요약·이용권 요약은 읽기 API로 응답한다")
    void summaryAndTicketSummary_areAvailable() throws Exception {
        MvcResult summaryResult = mockMvc.perform(get("/api/v1/admin/classes/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode summary = objectMapper.readTree(summaryResult.getResponse().getContentAsString()).get("data");
        assertThat(summary.get("totalClasses").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(summary.get("participants").asLong()).isGreaterThanOrEqualTo(2);

        MvcResult ticketSummaryResult = mockMvc.perform(get("/api/v1/admin/tickets/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode ticketSummary = objectMapper.readTree(ticketSummaryResult.getResponse().getContentAsString()).get("data");
        assertThat(ticketSummary.get("totalTickets").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(ticketSummary.get("inUse").asLong()).isGreaterThanOrEqualTo(2);
    }

    private Long createClass(String suffix, String name, LocalDateTime deadline) {
        User owner = newUser("owner-" + UUID.randomUUID().toString().substring(0, 8), UserRole.USER);
        LocalDateTime now = LocalDateTime.now();
        FundingOrder order = fundingOrderRepository.save(FundingOrder.builder()
                .platform(FundingPlatform.MANUAL)
                .campaignId("campaign-" + suffix)
                .orderId("order-" + UUID.randomUUID())
                .rewardTier(RewardTier.SEAT_5)
                .quantity(1)
                .supporterName("후원자")
                .paymentStatus(PaymentStatus.PAID_CONFIRMED)
                .paidAt(now.minusDays(2))
                .build());
        orderIds.add(order.getId());
        ClassTicket ticket = classTicketRepository.save(ClassTicket.issue(order, TicketCodeHasher.hash(UUID.randomUUID().toString())));
        ticket.recordCodeSuffix("PKE1234");
        ticket.register(owner, now.minusDays(1));
        classTicketRepository.save(ticket);
        ticketIds.add(ticket.getId());

        Battle battle = battleRepository.save(Battle.builder()
                .title("기존 토론 콘텐츠")
                .summary("요약")
                .status(BattleStatus.PUBLISHED)
                .creatorType(BattleCreatorType.ADMIN)
                .build());
        battleIds.add(battle.getId());

        Classroom classroom = classroomRepository.save(Classroom.builder()
                .name(name)
                .inviteCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .owner(owner)
                .ticket(ticket)
                .battle(battle)
                .battleType(ClassBattleType.TTS)
                .capacity(5)
                .deadline(deadline)
                .commentRequired(false)
                .ownerParticipates(true)
                .build());
        classIds.add(classroom.getId());
        classMemberRepository.save(ClassMember.builder()
                .classroom(classroom)
                .user(owner)
                .displayName("운영자")
                .role(ClassMemberRole.OWNER)
                .joinedAt(now)
                .build());
        return classroom.getId();
    }

    private User newUser(String tag, UserRole role) {
        return userRepository.save(User.builder()
                .userTag(tag)
                .nickname(tag)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
