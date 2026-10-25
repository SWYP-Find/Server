package com.swyp.picke.domain.classroom.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassReportRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.entity.ClassReport;
import com.swyp.picke.domain.classroom.enums.ClassReportType;
import com.swyp.picke.domain.classroom.service.retention.ClassDataPurgeService;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.perspective.repository.CommentLikeRepository;
import com.swyp.picke.domain.perspective.repository.CommentReportRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveLikeRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveReportRepository;
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
import java.time.ZoneId;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 클래스 안에서의 배틀 진행(상세 → 사전투표 → TTS → 사후투표 → 의견)과 일반 배틀 격리를 HTTP 로 검증한다.
 * 기준: PRD §3(일반 배틀과 분리), §7(투표 잠금), F07(재참여 시 진행 유지), F10(참여 완료 = 사후투표 + 의견), F15(나간 뒤 좌석 유지).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassBattleApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private FundingOrderRepository fundingOrderRepository;
    @Autowired
    private ClassTicketRepository classTicketRepository;
    @Autowired
    private BattleRepository battleRepository;
    @Autowired
    private BattleOptionRepository battleOptionRepository;
    @Autowired
    private ClassroomRepository classroomRepository;
    @Autowired
    private ClassMemberRepository classMemberRepository;
    @Autowired
    private BattleVoteRepository battleVoteRepository;
    @Autowired
    private UserBattleRepository userBattleRepository;
    @Autowired
    private PerspectiveRepository perspectiveRepository;
    @Autowired
    private PerspectiveCommentRepository perspectiveCommentRepository;
    @Autowired
    private PerspectiveLikeRepository perspectiveLikeRepository;
    @Autowired
    private CommentLikeRepository commentLikeRepository;
    @Autowired
    private PerspectiveReportRepository perspectiveReportRepository;
    @Autowired
    private CommentReportRepository commentReportRepository;
    @Autowired
    private ClassReportRepository classReportRepository;
    @Autowired
    private ClassDataPurgeService purgeService;

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
        issueAndRegister(owner, RewardTier.SEAT_5);
        battle = newBattle();
        optionA = newOption(battle, 1);
        optionB = newOption(battle, 2);
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
        commentReportRepository.deleteAll();
        perspectiveReportRepository.deleteAll();
        commentLikeRepository.deleteAll();
        perspectiveLikeRepository.deleteAll();
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
    @DisplayName("참여 → 상세 → 사전투표(변경) → TTS → 사전투표 잠김 → 사후투표(1회) → 의견 → 참여 완료까지 한 흐름")
    void fullFlow_inClass() throws Exception {
        ClassInfo cls = createClass(false);
        User student = newUser("학생A");
        String token = tokenOf(student);
        join(token, cls.inviteCode, "학생A").andExpect(status().isOk());

        // 상세: 아직 아무것도 안 함
        mockMvc.perform(get(classBattle(cls)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classId").value(cls.classId))
                .andExpect(jsonPath("$.data.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.data.closed").value(false))
                .andExpect(jsonPath("$.data.commentRequired").value(true))
                .andExpect(jsonPath("$.data.canParticipate").value(true))
                .andExpect(jsonPath("$.data.participationCompleted").value(false))
                .andExpect(jsonPath("$.data.battle.currentStep").value("NONE"))
                .andExpect(jsonPath("$.data.battle.battleInfo.participantsCount").value(0));

        // 사전투표 A → B 로 변경 (배틀 시작 전이므로 허용)
        vote(token, cls, "/votes/pre", optionA).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PRE_VOTE"));
        vote(token, cls, "/votes/pre", optionB).andExpect(status().isOk());
        mockMvc.perform(get(classBattle(cls) + "/votes/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preVote.optionId").value(optionB.getId()))
                .andExpect(jsonPath("$.data.status").value("PRE_VOTE"));

        // TTS 를 끝까지 듣기 전에는 사후투표 불가
        vote(token, cls, "/votes/post", optionA).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_TTS_INCOMPLETE"));

        // TTS 청취 완료 → 이후 사전투표 변경은 잠김
        mockMvc.perform(post(classBattle(cls) + "/votes/tts-complete").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        vote(token, cls, "/votes/pre", optionA).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_PRE_VOTE_LOCKED"));

        // 사후투표는 1회만
        vote(token, cls, "/votes/post", optionA).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        vote(token, cls, "/votes/post", optionB).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VOTE_409_SUB"));

        // 의견 필수 클래스: 사후투표만으로는 완료가 아니다
        mockMvc.perform(get(classBattle(cls)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.battle.currentStep").value("COMPLETED"))
                .andExpect(jsonPath("$.data.battle.userVoteStatus").value("PRO"))
                .andExpect(jsonPath("$.data.participationCompleted").value(false));

        // 의견 등록 → 참여 완료
        long perspectiveId = dataOf(mockMvc.perform(post(classBattle(cls) + "/perspectives")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "클래스 안에서 쓴 의견입니다. 충분히 긴 내용으로 작성합니다."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andReturn()).get("perspectiveId").asLong();

        // 의견 필수 클래스의 의견은 삭제 대신 수정만
        mockMvc.perform(delete("/api/v1/perspectives/" + perspectiveId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_PERSPECTIVE_REQUIRED"));
        mockMvc.perform(patch("/api/v1/perspectives/" + perspectiveId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "수정한 클래스 의견입니다. 충분히 긴 내용으로 다시 작성합니다."))))
                .andExpect(status().isOk());

        mockMvc.perform(get(classBattle(cls)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participationCompleted").value(true))
                .andExpect(jsonPath("$.data.battle.battleInfo.participantsCount").value(1));
        mockMvc.perform(get(classBattle(cls) + "/vote-stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1));
        mockMvc.perform(get(classBattle(cls) + "/perspectives").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)));
        mockMvc.perform(get(classBattle(cls) + "/perspectives/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
        mockMvc.perform(get(classBattle(cls) + "/votes/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postVote.optionId").value(optionA.getId()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("클래스 투표·의견은 같은 배틀의 일반 집계에 섞이지 않고, 일반 투표도 클래스에 영향을 주지 않는다")
    void classAndGeneral_areIsolated() throws Exception {
        ClassInfo cls = createClass(false);
        User student = newUser("학생A");
        String token = tokenOf(student);
        join(token, cls.inviteCode, "학생A").andExpect(status().isOk());

        vote(token, cls, "/votes/pre", optionA).andExpect(status().isOk());
        listen(token, cls);
        vote(token, cls, "/votes/post", optionA).andExpect(status().isOk());
        mockMvc.perform(post(classBattle(cls) + "/perspectives")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "클래스 의견은 일반 목록에 보이면 안 됩니다."))))
                .andExpect(status().isOk());

        // 일반 배틀 쪽에서는 아무것도 없다
        mockMvc.perform(get("/api/v1/battles/" + battle.getId() + "/votes/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VOTE_404"));
        mockMvc.perform(get("/api/v1/battles/" + battle.getId() + "/vote-stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0));
        mockMvc.perform(get("/api/v1/battles/" + battle.getId() + "/perspectives").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));
        assertThat(battleRepository.findById(battle.getId()).orElseThrow().getTotalParticipantsCount()).isZero();

        // 같은 사용자가 일반에서 사전투표하면 일반 참여자 수만 오르고 클래스는 그대로
        mockMvc.perform(post("/api/v1/battles/" + battle.getId() + "/votes/pre")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("optionId", optionB.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PRE_VOTE"));
        assertThat(battleRepository.findById(battle.getId()).orElseThrow().getTotalParticipantsCount()).isEqualTo(1L);
        mockMvc.perform(get(classBattle(cls) + "/votes/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preVote.optionId").value(optionA.getId()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(get(classBattle(cls)).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.battle.battleInfo.participantsCount").value(1));
    }

    @Test
    @DisplayName("비멤버는 403, 참여하지 않는 운영자는 읽기만 되고 투표는 403")
    void access_nonMember_and_nonParticipatingOwner() throws Exception {
        ClassInfo cls = createClass(false);
        String strangerToken = tokenOf(newUser("외부인"));

        mockMvc.perform(get(classBattle(cls)).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));

        mockMvc.perform(get(classBattle(cls)).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.canParticipate").value(false));
        mockMvc.perform(get(classBattle(cls) + "/vote-stats").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        vote(ownerToken, cls, "/votes/pre", optionA).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));
    }

    @Test
    @DisplayName("마감일이 지나면 투표·의견·댓글 쓰기는 409 CLASS_409_CLOSED, 조회는 가능하다")
    void closedClass_blocksWritesOnly() throws Exception {
        ClassInfo cls = createClass(true);
        User student = newUser("학생A");
        String token = tokenOf(student);
        join(token, cls.inviteCode, "학생A").andExpect(status().isOk());

        // 운영자(참여)가 먼저 의견까지 남겨 둔다
        vote(ownerToken, cls, "/votes/pre", optionA).andExpect(status().isOk());
        listen(ownerToken, cls);
        vote(ownerToken, cls, "/votes/post", optionA).andExpect(status().isOk());
        MvcResult created = mockMvc.perform(post(classBattle(cls) + "/perspectives")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "마감 전에 남긴 운영자 의견입니다."))))
                .andExpect(status().isOk())
                .andReturn();
        long perspectiveId = dataOf(created).get("perspectiveId").asLong();

        closeClass(cls.classId);

        vote(token, cls, "/votes/pre", optionA).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_CLOSED"));
        mockMvc.perform(post(classBattle(cls) + "/perspectives")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "마감 후 의견은 막혀야 합니다."))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_CLOSED"));
        mockMvc.perform(post("/api/v1/perspectives/" + perspectiveId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "마감 후 댓글도 막혀야 합니다."))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_CLOSED"));

        mockMvc.perform(get(classBattle(cls)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closed").value(true))
                .andExpect(jsonPath("$.data.canParticipate").value(false));
        mockMvc.perform(get(classBattle(cls) + "/perspectives").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("클래스 의견·댓글 좋아요: 비멤버 403, 참여하지 않는 운영자는 조회만, 마감 후 등록·취소 409")
    void likes_followClassAccess() throws Exception {
        ClassInfo cls = createClass(false);
        User writer = newUser("학생A");
        String writerToken = tokenOf(writer);
        User reader = newUser("학생B");
        String readerToken = tokenOf(reader);
        join(writerToken, cls.inviteCode, "학생A").andExpect(status().isOk());
        join(readerToken, cls.inviteCode, "학생B").andExpect(status().isOk());

        vote(writerToken, cls, "/votes/pre", optionA).andExpect(status().isOk());
        listen(writerToken, cls);
        vote(writerToken, cls, "/votes/post", optionA).andExpect(status().isOk());
        long perspectiveId = dataOf(mockMvc.perform(post(classBattle(cls) + "/perspectives")
                        .header("Authorization", "Bearer " + writerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "좋아요 권한을 확인할 클래스 의견입니다."))))
                .andExpect(status().isOk())
                .andReturn()).get("perspectiveId").asLong();
        long commentId = dataOf(mockMvc.perform(post("/api/v1/perspectives/" + perspectiveId + "/comments")
                        .header("Authorization", "Bearer " + writerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "좋아요 권한을 확인할 클래스 댓글입니다."))))
                .andExpect(status().isOk())
                .andReturn()).get("commentId").asLong();
        String perspectiveLikes = "/api/v1/perspectives/" + perspectiveId + "/likes";
        String commentLikes = "/api/v1/comments/" + commentId + "/likes";

        // 클래스 밖 사용자는 조회·등록 모두 403
        String strangerToken = tokenOf(newUser("외부인"));
        mockMvc.perform(get(perspectiveLikes).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));
        mockMvc.perform(post(perspectiveLikes).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));
        mockMvc.perform(post(commentLikes).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));

        // 참여하지 않는 운영자는 조회만
        mockMvc.perform(get(perspectiveLikes).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post(perspectiveLikes).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));

        // 참여 중인 멤버는 마감 전 등록·취소 가능
        mockMvc.perform(post(perspectiveLikes).header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1));
        mockMvc.perform(post(commentLikes).header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete(commentLikes).header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post(commentLikes).header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isOk());

        // 마감 후에는 조회만 되고 등록·취소는 409
        closeClass(cls.classId);
        mockMvc.perform(get(perspectiveLikes).header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1));
        mockMvc.perform(delete(perspectiveLikes).header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_CLOSED"));
        mockMvc.perform(delete(commentLikes).header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLASS_409_CLOSED"));
        assertThat(perspectiveLikeRepository.count()).isEqualTo(1);
        assertThat(commentLikeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("클래스 의견·댓글 신고: 비멤버 403, 참여하지 않는 운영자·멤버는 마감 후에도 신고 가능, 다른 관점 경로의 댓글은 404")
    void reports_requireClassReadAccess() throws Exception {
        ClassInfo cls = createClass(false);
        User writer = newUser("학생A");
        String writerToken = tokenOf(writer);
        User reader = newUser("학생B");
        String readerToken = tokenOf(reader);
        join(writerToken, cls.inviteCode, "학생A").andExpect(status().isOk());
        join(readerToken, cls.inviteCode, "학생B").andExpect(status().isOk());

        vote(writerToken, cls, "/votes/pre", optionA).andExpect(status().isOk());
        listen(writerToken, cls);
        vote(writerToken, cls, "/votes/post", optionA).andExpect(status().isOk());
        long perspectiveId = dataOf(mockMvc.perform(post(classBattle(cls) + "/perspectives")
                        .header("Authorization", "Bearer " + writerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "신고 권한을 확인할 클래스 의견입니다."))))
                .andExpect(status().isOk())
                .andReturn()).get("perspectiveId").asLong();
        long commentId = dataOf(mockMvc.perform(post("/api/v1/perspectives/" + perspectiveId + "/comments")
                        .header("Authorization", "Bearer " + writerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "신고 권한을 확인할 클래스 댓글입니다."))))
                .andExpect(status().isOk())
                .andReturn()).get("commentId").asLong();
        String perspectiveReports = "/api/v1/perspectives/" + perspectiveId + "/reports";
        String commentReports = "/api/v1/perspectives/" + perspectiveId + "/comments/" + commentId + "/reports";

        // 클래스 밖 사용자는 403
        String strangerToken = tokenOf(newUser("외부인"));
        mockMvc.perform(post(perspectiveReports).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));
        mockMvc.perform(post(commentReports).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLASS_403_MEMBER"));

        // 댓글이 경로의 관점에 속하지 않으면 404
        mockMvc.perform(post("/api/v1/perspectives/" + (perspectiveId + 1000) + "/comments/" + commentId + "/reports")
                        .header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMENT_NOT_FOUND"));

        // 참여하지 않는 운영자도 볼 수 있는 글이라 신고 가능
        mockMvc.perform(post(perspectiveReports).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // 마감 후에도 멤버는 신고 가능
        closeClass(cls.classId);
        mockMvc.perform(post(perspectiveReports).header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post(commentReports).header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isOk());
        assertThat(perspectiveReportRepository.count()).isEqualTo(2);
        assertThat(commentReportRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("데이터 파기: 읽기 유예 중에는 그대로, 이용권 종료 + 30일이 지나면 의견·댓글·좋아요·신고·리포트를 지우고 투표·멤버는 남긴다")
    void purge_afterReadGrace() throws Exception {
        ClassInfo cls = createClass(false);
        User writer = newUser("학생A");
        String writerToken = tokenOf(writer);
        User reader = newUser("학생B");
        String readerToken = tokenOf(reader);
        join(writerToken, cls.inviteCode, "학생A").andExpect(status().isOk());
        join(readerToken, cls.inviteCode, "학생B").andExpect(status().isOk());

        vote(writerToken, cls, "/votes/pre", optionA).andExpect(status().isOk());
        listen(writerToken, cls);
        vote(writerToken, cls, "/votes/post", optionA).andExpect(status().isOk());
        long perspectiveId = dataOf(mockMvc.perform(post(classBattle(cls) + "/perspectives")
                        .header("Authorization", "Bearer " + writerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "파기될 클래스 의견입니다."))))
                .andExpect(status().isOk())
                .andReturn()).get("perspectiveId").asLong();
        long commentId = dataOf(mockMvc.perform(post("/api/v1/perspectives/" + perspectiveId + "/comments")
                        .header("Authorization", "Bearer " + readerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "파기될 클래스 댓글입니다."))))
                .andExpect(status().isOk())
                .andReturn()).get("commentId").asLong();
        mockMvc.perform(post("/api/v1/perspectives/" + perspectiveId + "/likes").header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/comments/" + commentId + "/likes").header("Authorization", "Bearer " + writerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/perspectives/" + perspectiveId + "/reports").header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isOk());
        Classroom classroom = classroomRepository.findById(cls.classId).orElseThrow();
        classReportRepository.save(ClassReport.builder().classroom(classroom).type(ClassReportType.CLASS_SUMMARY).build());

        // 이용권 사용 중: 대상 아님
        LocalDateTime now = LocalDateTime.now();
        assertThat(purgeService.findTargets(now, 10)).doesNotContain(cls.classId);
        assertThat(purgeService.purge(cls.classId, now)).isFalse();
        assertThat(perspectiveRepository.count()).isEqualTo(1);

        // 이용권 종료 + 30일 경과
        ClassTicket ticket = classTicketRepository.findById(classroom.getTicket().getId()).orElseThrow();
        ReflectionTestUtils.setField(ticket, "expiresAt", now.minusDays(ClassTicket.READ_GRACE_DAYS).minusMinutes(1));
        classTicketRepository.save(ticket);

        assertThat(purgeService.findTargets(now, 10)).contains(cls.classId);
        assertThat(purgeService.purge(cls.classId, now)).isTrue();

        assertThat(perspectiveRepository.count()).isZero();
        assertThat(perspectiveCommentRepository.count()).isZero();
        assertThat(perspectiveLikeRepository.count()).isZero();
        assertThat(commentLikeRepository.count()).isZero();
        assertThat(perspectiveReportRepository.count()).isZero();
        assertThat(classReportRepository.count()).isZero();
        assertThat(battleVoteRepository.count()).isEqualTo(1);
        assertThat(classMemberRepository.findByClassroomIdAndUserId(cls.classId, writer.getId())).isPresent();
        assertThat(classroomRepository.findById(cls.classId).orElseThrow().getPurgedAt()).isNotNull();

        // 이미 파기한 클래스는 다시 대상이 되지 않는다
        assertThat(purgeService.findTargets(now, 10)).doesNotContain(cls.classId);
        assertThat(purgeService.purge(cls.classId, now)).isFalse();
    }

    @Test
    @DisplayName("투표한 뒤 나가면 좌석은 유지(F15)되고, 재참여하면 진행 상태가 이어진다(F07)")
    void leaveAfterActivity_keepsSeat_andRejoinKeepsProgress() throws Exception {
        ClassInfo cls = createClass(false);
        User student = newUser("학생A");
        String token = tokenOf(student);
        join(token, cls.inviteCode, "학생A").andExpect(status().isOk());
        vote(token, cls, "/votes/pre", optionA).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/classes/" + cls.classId + "/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        ClassMember left = classMemberRepository.findByClassroomIdAndUserId(cls.classId, student.getId()).orElseThrow();
        assertThat(left.getStatus()).isEqualTo(ClassMemberStatus.LEFT);
        assertThat(left.isSeatHeld()).isTrue();

        mockMvc.perform(get(classBattle(cls)).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        join(token, cls.inviteCode, "학생A").andExpect(status().isOk());
        mockMvc.perform(get(classBattle(cls) + "/votes/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preVote.optionId").value(optionA.getId()))
                .andExpect(jsonPath("$.data.status").value("PRE_VOTE"));
    }

    // ===== 헬퍼 =====

    private record ClassInfo(long classId, String inviteCode) {
    }

    private static String classBattle(ClassInfo cls) {
        return "/api/v1/classes/" + cls.classId + "/battle";
    }

    private ResultActions vote(String token, ClassInfo cls, String path, BattleOption option) throws Exception {
        return mockMvc.perform(post(classBattle(cls) + path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("optionId", option.getId()))));
    }

    private void listen(String token, ClassInfo cls) throws Exception {
        mockMvc.perform(post(classBattle(cls) + "/votes/tts-complete").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void closeClass(long classId) {
        Classroom classroom = classroomRepository.findById(classId).orElseThrow();
        ReflectionTestUtils.setField(classroom, "deadline", LocalDateTime.now().minusMinutes(1));
        classroomRepository.save(classroom);
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

    private ResultActions join(String token, String code, String name) throws Exception {
        return mockMvc.perform(post("/api/v1/classes/join")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("inviteCode", code, "displayName", name))));
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode dataOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private User newUser(String nickname) {
        User user = userRepository.save(User.builder()
                .userTag("cb-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        // 의견 목록은 작성자 프로필(캐릭터)을 읽으므로 프로필까지 만들어 둔다
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
                // 서버는 KST 기준으로 오늘 배틀인지 본다. 시스템 시간대(CI 는 UTC)를 쓰면 KST 오전에 날짜가 어긋난다
                .targetDate(LocalDate.now(ZoneId.of("Asia/Seoul")))
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
