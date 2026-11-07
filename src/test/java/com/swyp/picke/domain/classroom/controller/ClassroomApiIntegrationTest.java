package com.swyp.picke.domain.classroom.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
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
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * 이용권 보유자가 클래스를 만들고 → 목록/상세 → 수정 → 삭제하는 흐름을 HTTP 로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassroomApiIntegrationTest {

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

    private final List<Long> createdBattleIds = new ArrayList<>();
    private User owner;
    private String ownerToken;
    private ClassTicket ownerTicket;
    private Battle battle;

    @BeforeEach
    void setUp() {
        cleanUp();
        owner = newUser("선생님");
        ownerToken = tokenOf(owner);
        ownerTicket = issueAndRegister(owner, RewardTier.SEAT_5);
        battle = newBattle(BattleStatus.PUBLISHED);
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
    @DisplayName("생성 → 내 목록 → 상세까지 한 흐름으로 동작하고 운영자에게는 초대코드가 내려간다")
    void create_thenList_thenDetail() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody("3학년 2반", battle.getId(), null, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("3학년 2반"))
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.inviteCode").isString())
                .andExpect(jsonPath("$.data.capacity").value(5))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.battleType").value("TTS"))
                .andExpect(jsonPath("$.data.battle.battleId").value(battle.getId()))
                .andExpect(jsonPath("$.data.closed").value(false))
                .andReturn();
        long classId = dataOf(created).get("classId").asLong();
        String inviteCode = dataOf(created).get("inviteCode").asText();
        assertThat(inviteCode).hasSize(Classroom.INVITE_CODE_LENGTH);

        mockMvc.perform(get("/api/v1/classes").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].classId").value(classId))
                .andExpect(jsonPath("$.data[0].myRole").value("OWNER"))
                .andExpect(jsonPath("$.data[0].memberCount").value(1))
                .andExpect(jsonPath("$.data[0].battleTitle").value(battle.getTitle()));

        mockMvc.perform(get("/api/v1/classes/" + classId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteCode").value(inviteCode))
                .andExpect(jsonPath("$.data.ownerId").value(owner.getId()))
                .andExpect(jsonPath("$.data.deadline").doesNotExist())
                .andExpect(jsonPath("$.data.ticketExpiresAt").exists());

        Classroom saved = classroomRepository.findById(classId).orElseThrow();
        assertThat(saved.getTicket().getId()).isEqualTo(ownerTicket.getId());
        assertThat(classMemberRepository.countByClassroomIdAndSeatHeldTrue(classId)).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 생성 요청을 연속으로 보내면 클래스는 하나만 생기고 같은 클래스를 돌려준다")
    void create_duplicateRequest_returnsSameClass() throws Exception {
        String body = objectMapper.writeValueAsString(createBody("3학년 2반", battle.getId(), null, true));
        long first = dataOf(mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn()).get("classId").asLong();
        long second = dataOf(mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andReturn()).get("classId").asLong();

        assertThat(second).isEqualTo(first);
        assertThat(classroomRepository.count()).isEqualTo(1);
        assertThat(classMemberRepository.count()).isEqualTo(1);

        // 이름이 다르면 별개 요청이라 새로 만든다
        mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody("3학년 3반", battle.getId(), null, true))))
                .andExpect(status().isOk());
        assertThat(classroomRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("요청 키: 같은 키·같은 내용은 기존 클래스, 같은 키·다른 내용은 409, 다른 키는 새 클래스")
    void create_withRequestKey() throws Exception {
        Map<String, Object> body = createBody("3학년 2반", battle.getId(), null, true);
        body.put("requestKey", "key-1");
        long first = dataOf(createClass(body).andExpect(status().isOk()).andReturn()).get("classId").asLong();

        // 재전송: 앞뒤 공백은 서버가 정규화하므로 같은 내용이다
        body.put("name", " 3학년 2반 ");
        long retried = dataOf(createClass(body).andExpect(status().isOk()).andReturn()).get("classId").asLong();
        assertThat(retried).isEqualTo(first);
        assertThat(classroomRepository.count()).isEqualTo(1);

        body.put("name", "3학년 3반");
        createClass(body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_REQUEST_KEY"));

        // 키가 다르면 같은 내용이어도 운영자가 의도한 새 클래스다 (10초 판정은 키가 없을 때만)
        body.put("name", "3학년 2반");
        body.put("requestKey", "key-2");
        long other = dataOf(createClass(body).andExpect(status().isOk()).andReturn()).get("classId").asLong();
        assertThat(other).isNotEqualTo(first);
        assertThat(classroomRepository.count()).isEqualTo(2);

        body.put("requestKey", "한글키");
        createClass(body).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("같은 요청 키로 동시에 들어와도 클래스는 하나만 생긴다")
    void create_concurrentSameRequestKey_createOnlyOne() throws Exception {
        Map<String, Object> body = createBody("3학년 2반", battle.getId(), null, true);
        body.put("requestKey", "concurrent-key");
        String json = objectMapper.writeValueAsString(body);
        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return dataOf(mockMvc.perform(post("/api/v1/classes")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isOk()).andReturn()).get("classId").asLong();
            }));
        }
        start.countDown();
        Set<Long> classIds = new HashSet<>();
        for (Future<Long> result : results) {
            classIds.add(result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(classIds).hasSize(1);
        assertThat(classroomRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 생성 요청이 동시에 들어와도 클래스는 하나만 생긴다")
    void create_concurrentDuplicateRequests_createOnlyOne() throws Exception {
        String body = objectMapper.writeValueAsString(createBody("3학년 2반", battle.getId(), null, true));
        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return dataOf(mockMvc.perform(post("/api/v1/classes")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isOk()).andReturn()).get("classId").asLong();
            }));
        }
        start.countDown();
        Set<Long> classIds = new HashSet<>();
        for (Future<Long> result : results) {
            classIds.add(result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(classIds).hasSize(1);
        assertThat(classroomRepository.count()).isEqualTo(1);
        assertThat(classMemberRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("운영자가 참여하지 않으면 멤버 행 없이 만들어지고 목록에는 운영자로 뜬다")
    void create_withoutOwnerParticipating() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody("클래스", battle.getId(), null, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(0))
                .andReturn();
        long classId = dataOf(created).get("classId").asLong();

        assertThat(classMemberRepository.count()).isZero();
        mockMvc.perform(get("/api/v1/classes").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].classId").value(classId))
                .andExpect(jsonPath("$.data[0].myRole").value("OWNER"));
    }

    @Test
    @DisplayName("이용권이 없으면 403 CLASS_403_TICKET, 마감일이 이용권 종료일을 넘으면 400 CLASS_400_DEADLINE, 빈 이름은 400")
    void create_validation() throws Exception {
        String noTicketToken = tokenOf(newUser("무이용권"));
        mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + noTicketToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody("클래스", battle.getId(), null, false))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_TICKET"));

        LocalDateTime tooLate = ownerTicket.getExpiresAt().plusDays(1);
        mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody("클래스", battle.getId(), tooLate, false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_DEADLINE"));

        mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody("   ", battle.getId(), null, false))))
                .andExpect(status().isBadRequest());

        Battle pending = newBattle(BattleStatus.PENDING);
        mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody("클래스", pending.getId(), null, false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_BATTLE"));

        assertThat(classroomRepository.count()).isZero();
    }

    @Test
    @DisplayName("멤버는 상세를 볼 수 있지만 초대코드는 못 보고, 외부인은 403 CLASS_403_MEMBER")
    void detail_memberAndOutsider() throws Exception {
        long classId = createClass("클래스", null, false);
        Classroom classroom = classroomRepository.findById(classId).orElseThrow();
        User student = newUser("학생");
        classMemberRepository.save(ClassMember.builder()
                .classroom(classroom).user(student).displayName("학생")
                .role(ClassMemberRole.MEMBER).joinedAt(LocalDateTime.now()).build());
        String studentToken = tokenOf(student);

        mockMvc.perform(get("/api/v1/classes/" + classId).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.data.inviteCode").value(classroom.getInviteCode()))
                .andExpect(jsonPath("$.data.memberCount").value(1));

        mockMvc.perform(get("/api/v1/classes").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].myRole").value("MEMBER"));

        String outsiderToken = tokenOf(newUser("외부인"));
        mockMvc.perform(get("/api/v1/classes/" + classId).header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));
        mockMvc.perform(get("/api/v1/classes").header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("운영자는 이름 변경·마감일 연장이 되고, 멤버가 아닌 유저는 403 CLASS_403_OWNER")
    void update_ownerOnly() throws Exception {
        LocalDateTime deadline = LocalDateTime.now().plusDays(10).withNano(0);
        long classId = createClass("클래스", deadline, false);
        LocalDateTime extended = deadline.plusDays(7);

        mockMvc.perform(patch("/api/v1/classes/" + classId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "새 이름", "deadline", extended.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("새 이름"))
                .andExpect(jsonPath("$.data.deadline").value(extended.toString()))
                .andExpect(jsonPath("$.data.closeAt").value(extended.toString()));

        mockMvc.perform(patch("/api/v1/classes/" + classId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deadline", deadline.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_DEADLINE_EXT"));

        mockMvc.perform(patch("/api/v1/classes/" + classId)
                        .header("Authorization", "Bearer " + tokenOf(newUser("남")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "해킹"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_OWNER"));

        Classroom saved = classroomRepository.findById(classId).orElseThrow();
        assertThat(saved.getName()).isEqualTo("새 이름");
        assertThat(saved.getDeadline()).isEqualTo(extended);
    }

    @Test
    @DisplayName("삭제하면 소프트 삭제되고 목록·상세에서 사라진다")
    void delete_softDeletes() throws Exception {
        long classId = createClass("클래스", null, true);

        mockMvc.perform(delete("/api/v1/classes/" + classId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/classes/" + classId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CLASS_404"));
        mockMvc.perform(get("/api/v1/classes").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        Classroom saved = classroomRepository.findById(classId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ClassStatus.DELETED);
        assertThat(saved.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("토큰이 없으면 401")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/classes")).andExpect(status().isUnauthorized());
    }

    // ===== 헬퍼 =====

    private long createClass(String name, LocalDateTime deadline, boolean ownerParticipates) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody(name, battle.getId(), deadline, ownerParticipates))))
                .andExpect(status().isOk())
                .andReturn();
        return dataOf(result).get("classId").asLong();
    }

    private static Map<String, Object> createBody(String name, Long battleId, LocalDateTime deadline, boolean ownerParticipates) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("battleId", battleId);
        body.put("deadline", deadline == null ? null : deadline.toString());
        body.put("commentRequired", true);
        body.put("ownerParticipates", ownerParticipates);
        return body;
    }

    private org.springframework.test.web.servlet.ResultActions createClass(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private JsonNode dataOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private User newUser(String nickname) {
        return userRepository.save(User.builder()
                .userTag("cls-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private String tokenOf(User user) {
        return jwtProvider.createAccessToken(user.getId(), UserRole.USER.name());
    }

    private ClassTicket issueAndRegister(User user, RewardTier tier) {
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
        return classTicketRepository.save(ticket);
    }

    private Battle newBattle(BattleStatus status) {
        Battle saved = battleRepository.save(Battle.builder()
                .title("클래스 배틀 " + UUID.randomUUID().toString().substring(0, 4))
                .summary("요약")
                .description("설명")
                .thumbnailUrl("https://img/class.png")
                .targetDate(LocalDate.now())
                .status(status)
                .creatorType(BattleCreatorType.ADMIN)
                .build());
        createdBattleIds.add(saved.getId());
        return saved;
    }
}
