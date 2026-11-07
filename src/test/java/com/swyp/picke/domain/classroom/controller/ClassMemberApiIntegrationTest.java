package com.swyp.picke.domain.classroom.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.dto.request.ClassJoinRequest;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.service.ClassMemberService;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.repository.FundingOrderRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 초대코드 참여 → 명단 → 이름 변경 → 나가기/내보내기 흐름을 HTTP 로 검증한다.
 * 마지막 1석 동시 참여는 서비스 직접 호출로 검증한다(트랜잭션 2개를 실제 스레드로 돌리기 위해).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassMemberApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FundingOrderRepository fundingOrderRepository;
    @Autowired
    private ClassTicketRepository classTicketRepository;
    @Autowired
    private BattleRepository battleRepository;
    @Autowired
    private ClassroomRepository classroomRepository;
    @Autowired
    private ClassMemberRepository classMemberRepository;
    @Autowired
    private ClassMemberService classMemberService;

    private final List<Long> createdBattleIds = new ArrayList<>();
    private User owner;
    private String ownerToken;
    private Battle battle;

    @BeforeEach
    void setUp() {
        cleanUp();
        owner = newUser("선생님");
        ownerToken = tokenOf(owner);
        issueAndRegister(owner, RewardTier.SEAT_5);
        battle = newBattle();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        battleRepository.deleteAllById(createdBattleIds);
        createdBattleIds.clear();
    }

    private void cleanUp() {
        classMemberRepository.deleteAll();
        classroomRepository.deleteAll();
        classTicketRepository.deleteAll();
        fundingOrderRepository.deleteAll();
    }

    @Test
    @DisplayName("참여 → 명단 → 이름 변경 → 나가기 → 재참여까지 한 흐름으로 동작한다")
    void join_thenMembers_thenRename_thenLeave_thenRejoin() throws Exception {
        ClassInfo cls = createClass(true);
        User student = newUser("학생A");
        String studentToken = tokenOf(student);

        // 소문자·공백 섞인 코드도 정규화되어 들어간다. 멤버도 공유하기용 초대코드를 받는다
        mockMvc.perform(post("/api/v1/classes/join")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("inviteCode", " " + cls.inviteCode.toLowerCase() + " ", "displayName", " 학생A "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classId").value(cls.classId))
                .andExpect(jsonPath("$.data.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.data.inviteCode").value(cls.inviteCode))
                .andExpect(jsonPath("$.data.memberCount").value(2));

        // 명단: 운영자 먼저, 본인 행 me=true, userId 같은 계정 식별자 없음
        MvcResult members = mockMvc.perform(get("/api/v1/classes/" + cls.classId + "/members")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data[0].me").value(false))
                .andExpect(jsonPath("$.data[1].displayName").value("학생A"))
                .andExpect(jsonPath("$.data[1].role").value("MEMBER"))
                .andExpect(jsonPath("$.data[1].me").value(true))
                .andExpect(jsonPath("$.data[1].userId").doesNotExist())
                .andReturn();
        long myMemberId = dataOf(members).get(1).get("memberId").asLong();

        // 내 목록에도 MEMBER 로 뜬다
        mockMvc.perform(get("/api/v1/classes").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].myRole").value("MEMBER"));

        // 이름 변경
        mockMvc.perform(patch("/api/v1/classes/" + cls.classId + "/members/me")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("displayName", "학생B"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(myMemberId))
                .andExpect(jsonPath("$.data.displayName").value("학생B"));

        // 나가기 → 상세 403, 좌석 반환
        mockMvc.perform(delete("/api/v1/classes/" + cls.classId + "/members/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/classes/" + cls.classId).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));
        ClassMember left = classMemberRepository.findById(myMemberId).orElseThrow();
        assertThat(left.getStatus()).isEqualTo(ClassMemberStatus.LEFT);
        assertThat(left.isSeatHeld()).isFalse();
        assertThat(classMemberRepository.countByClassroomIdAndSeatHeldTrue(cls.classId)).isEqualTo(1);

        // 재참여: 같은 행이 되살아난다 (행 수 그대로)
        mockMvc.perform(post("/api/v1/classes/join")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("inviteCode", cls.inviteCode, "displayName", "학생C"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(2));
        ClassMember rejoined = classMemberRepository.findById(myMemberId).orElseThrow();
        assertThat(rejoined.getStatus()).isEqualTo(ClassMemberStatus.ACTIVE);
        assertThat(rejoined.getDisplayName()).isEqualTo("학생C");
        assertThat(rejoined.isSeatHeld()).isTrue();
        assertThat(classMemberRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("이미 참여 중인 계정이 다시 참여하면 200 + 기존 상세이고 멤버 수가 늘지 않는다")
    void join_again_returnsExistingWithoutNewRow() throws Exception {
        ClassInfo cls = createClass(false);
        String studentToken = tokenOf(newUser("학생"));

        join(studentToken, cls.inviteCode, "학생").andExpect(status().isOk());
        join(studentToken, cls.inviteCode, "다른이름")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.data.memberCount").value(1));

        assertThat(classMemberRepository.count()).isEqualTo(1);
        assertThat(classMemberRepository.findActiveMembers(cls.classId).get(0).getDisplayName()).isEqualTo("학생");
    }

    @Test
    @DisplayName("참여 거절: 잘못된 코드 400, 없는 코드 404, 운영자 본인 409, 이름 중복 409, 정원 초과 409, 마감 409")
    void join_rejections() throws Exception {
        ClassInfo cls = createClass(true); // 5인권, 운영자 1석

        join(tokenOf(newUser("x")), "ABC", "이름")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));
        join(tokenOf(newUser("x")), "ZZZZZZ", "이름")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CLASS_404_CODE"));
        join(ownerToken, cls.inviteCode, "선생님")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_OWNER"));

        join(tokenOf(newUser("a")), cls.inviteCode, "같은이름").andExpect(status().isOk());
        join(tokenOf(newUser("b")), cls.inviteCode, " 같은이름 ")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_NAME"));

        join(tokenOf(newUser("c")), cls.inviteCode, "셋").andExpect(status().isOk());
        join(tokenOf(newUser("d")), cls.inviteCode, "넷").andExpect(status().isOk());
        join(tokenOf(newUser("e")), cls.inviteCode, "다섯").andExpect(status().isOk());
        join(tokenOf(newUser("f")), cls.inviteCode, "여섯")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_FULL"));
        assertThat(classMemberRepository.countByClassroomIdAndSeatHeldTrue(cls.classId)).isEqualTo(5);

        // 마감된 클래스: 마감일을 과거로 돌려 놓고 참여 시도
        Classroom classroom = classroomRepository.findById(cls.classId).orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(classroom, "deadline", LocalDateTime.now().minusMinutes(1));
        classroomRepository.save(classroom);
        join(tokenOf(newUser("g")), cls.inviteCode, "일곱")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_CLOSED"));

        // 이용권 회수: 마감일과 무관하게 "이용 종료" 로 구분 거절 (PRD §15)
        var ticket = classTicketRepository.findById(classroom.getTicket().getId()).orElseThrow();
        ticket.revoke(LocalDateTime.now().minusMinutes(1));
        classTicketRepository.save(ticket);
        join(tokenOf(newUser("h")), cls.inviteCode, "여덟")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_TICKET_ENDED"));

        mockMvc.perform(post("/api/v1/classes/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("inviteCode", cls.inviteCode, "displayName", "익명"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("운영자가 내보내면 REMOVED 가 되고 다시 참여할 수 없다. 운영자 본인 행·외부인 내보내기는 거절")
    void removeMember_andRejoinBlocked() throws Exception {
        ClassInfo cls = createClass(true);
        User student = newUser("학생");
        String studentToken = tokenOf(student);
        join(studentToken, cls.inviteCode, "학생").andExpect(status().isOk());
        long studentMemberId = memberIdOf(cls.classId, student);
        long ownerMemberId = memberIdOf(cls.classId, owner);

        // 멤버는 남을 내보낼 수 없다
        mockMvc.perform(delete("/api/v1/classes/" + cls.classId + "/members/" + ownerMemberId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_OWNER"));
        // 운영자 본인 행은 내보낼 수 없다
        mockMvc.perform(delete("/api/v1/classes/" + cls.classId + "/members/" + ownerMemberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_OWNER_LEAVE"));
        // 운영자 나가기도 불가
        mockMvc.perform(delete("/api/v1/classes/" + cls.classId + "/members/me")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_OWNER_LEAVE"));

        mockMvc.perform(delete("/api/v1/classes/" + cls.classId + "/members/" + studentMemberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        ClassMember removed = classMemberRepository.findById(studentMemberId).orElseThrow();
        assertThat(removed.getStatus()).isEqualTo(ClassMemberStatus.REMOVED);
        assertThat(removed.isSeatHeld()).isFalse();

        join(studentToken, cls.inviteCode, "학생")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_REMOVED"));
        mockMvc.perform(get("/api/v1/classes/" + cls.classId + "/members")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));
        mockMvc.perform(get("/api/v1/classes/" + cls.classId + "/members")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 이미 내보낸 멤버를 다시 내보내면 404
        mockMvc.perform(delete("/api/v1/classes/" + cls.classId + "/members/" + studentMemberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CLASS_404_MEMBER"));
    }

    @Test
    @DisplayName("참여 코드 확인: 참여 가능하면 클래스 요약(멤버는 만들지 않음), 이미 참여 중·내 클래스·정원 초과는 사유별 409")
    void previewJoin_summaryAndRejections() throws Exception {
        ClassInfo cls = createClass(true); // 5인권, 운영자 1석

        preview(tokenOf(newUser("x")), " " + cls.inviteCode.toLowerCase() + " ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteCode").value(cls.inviteCode))
                .andExpect(jsonPath("$.data.name").value("클래스"))
                .andExpect(jsonPath("$.data.battleTitle").value(battle.getTitle()))
                .andExpect(jsonPath("$.data.battleType").value("TTS"))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.capacity").value(5));
        assertThat(classMemberRepository.countByClassroomIdAndSeatHeldTrue(cls.classId)).isEqualTo(1);

        preview(tokenOf(newUser("x")), "ABC")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));
        preview(tokenOf(newUser("x")), "ZZZZZZ")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CLASS_404_CODE"));
        preview(ownerToken, cls.inviteCode)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_OWNER"));

        String joinedToken = tokenOf(newUser("a"));
        join(joinedToken, cls.inviteCode, "하나").andExpect(status().isOk());
        preview(joinedToken, cls.inviteCode)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_JOINED"));

        join(tokenOf(newUser("b")), cls.inviteCode, "둘").andExpect(status().isOk());
        join(tokenOf(newUser("c")), cls.inviteCode, "셋").andExpect(status().isOk());
        join(tokenOf(newUser("d")), cls.inviteCode, "넷").andExpect(status().isOk());
        preview(tokenOf(newUser("e")), cls.inviteCode)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_FULL"));
    }

    @Test
    @DisplayName("참여 코드 확인: 내보내진 클래스 403, 마감 409, 이용권 종료 409, 삭제된 클래스는 확인·참여 모두 409 CLASS_409_DELETED")
    void previewJoin_removedClosedDeleted() throws Exception {
        ClassInfo cls = createClass(true);
        User student = newUser("학생");
        String studentToken = tokenOf(student);
        join(studentToken, cls.inviteCode, "학생").andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/classes/" + cls.classId + "/members/" + memberIdOf(cls.classId, student))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        preview(studentToken, cls.inviteCode)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_REMOVED"));

        Classroom classroom = classroomRepository.findById(cls.classId).orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(classroom, "deadline", LocalDateTime.now().minusMinutes(1));
        classroomRepository.save(classroom);
        preview(tokenOf(newUser("g")), cls.inviteCode)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_CLOSED"));

        var ticket = classTicketRepository.findById(classroom.getTicket().getId()).orElseThrow();
        ticket.revoke(LocalDateTime.now().minusMinutes(1));
        classTicketRepository.save(ticket);
        preview(tokenOf(newUser("h")), cls.inviteCode)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_TICKET_ENDED"));

        mockMvc.perform(delete("/api/v1/classes/" + cls.classId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        String outsider = tokenOf(newUser("i"));
        preview(outsider, cls.inviteCode)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_DELETED"));
        join(outsider, cls.inviteCode, "아홉")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_DELETED"));
    }

    @Test
    @DisplayName("마지막 1석을 두 사람이 동시에 신청하면 한 명만 들어간다 (클래스 행 잠금)")
    void join_lastSeat_concurrent_onlyOneSucceeds() throws Exception {
        ClassInfo cls = createClass(true); // 운영자 1석
        for (int i = 0; i < 3; i++) {
            join(tokenOf(newUser("m" + i)), cls.inviteCode, "멤버" + i).andExpect(status().isOk());
        }
        assertThat(classMemberRepository.countByClassroomIdAndSeatHeldTrue(cls.classId)).isEqualTo(4);
        Long userA = newUser("A").getId();
        Long userB = newUser("B").getId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<ErrorCode>> results = new ArrayList<>();
        for (Long userId : List.of(userA, userB)) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                try {
                    classMemberService.join(userId, new ClassJoinRequest(cls.inviteCode, "유저" + userId));
                    return null;
                } catch (CustomException e) {
                    return e.getErrorCode();
                }
            }));
        }
        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        List<ErrorCode> outcomes = new ArrayList<>();
        for (Future<ErrorCode> f : results) {
            outcomes.add(f.get(10, TimeUnit.SECONDS));
        }
        pool.shutdownNow();

        assertThat(outcomes).containsExactlyInAnyOrder(null, ErrorCode.CLASS_FULL);
        assertThat(classMemberRepository.countByClassroomIdAndSeatHeldTrue(cls.classId)).isEqualTo(5);
    }

    // ===== 헬퍼 =====

    private record ClassInfo(long classId, String inviteCode) {
    }

    private ClassInfo createClass(boolean ownerParticipates) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "클래스");
        body.put("battleId", battle.getId());
        body.put("deadline", null);
        body.put("commentRequired", true);
        body.put("ownerParticipates", ownerParticipates);
        MvcResult result = mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = dataOf(result);
        return new ClassInfo(data.get("classId").asLong(), data.get("inviteCode").asText());
    }

    private org.springframework.test.web.servlet.ResultActions join(String token, String code, String name) throws Exception {
        return mockMvc.perform(post("/api/v1/classes/join")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("inviteCode", code, "displayName", name))));
    }

    private org.springframework.test.web.servlet.ResultActions preview(String token, String code) throws Exception {
        return mockMvc.perform(get("/api/v1/classes/join/preview")
                .header("Authorization", "Bearer " + token)
                .param("inviteCode", code));
    }

    private long memberIdOf(long classId, User user) {
        return classMemberRepository.findByClassroomIdAndUserId(classId, user.getId()).orElseThrow().getId();
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode dataOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private User newUser(String nickname) {
        return userRepository.save(User.builder()
                .userTag("mem-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private String tokenOf(User user) {
        return jwtProvider.createAccessToken(user.getId(), UserRole.USER.name());
    }

    private void issueAndRegister(User user, RewardTier tier) {
        FundingOrder order = fundingOrderRepository.save(FundingOrder.builder()
                .platform(FundingPlatform.TUMBLBUG)
                .campaignId("camp-test")
                .orderId("order-" + UUID.randomUUID())
                .rewardTier(tier)
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
}
