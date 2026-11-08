package com.swyp.picke.domain.perspective.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveLike;
import com.swyp.picke.domain.perspective.repository.PerspectiveLikeRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserBattle;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserBattleRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 주간 베스트 관점(#626)을 HTTP 로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeeklyBestPerspectiveApiIntegrationTest {

    private static final String URL = "/api/v1/perspectives/weekly-best";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private UserBattleRepository userBattleRepository;
    @Autowired private BattleRepository battleRepository;
    @Autowired private BattleOptionRepository battleOptionRepository;
    @Autowired private PerspectiveRepository perspectiveRepository;
    @Autowired private PerspectiveLikeRepository perspectiveLikeRepository;

    private final List<Long> userBattleIds = new ArrayList<>();
    private final List<Long> likeIds = new ArrayList<>();
    private final List<Long> perspectiveIds = new ArrayList<>();
    private final List<Long> optionIds = new ArrayList<>();
    private final List<Long> battleIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    private LocalDateTime weekStart;
    private LocalDateTime weekEnd;
    private User viewer;
    private User liker1;
    private User liker2;

    @BeforeEach
    void setUp() {
        LocalDate thisMonday = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        weekStart = thisMonday.minusWeeks(1).atStartOfDay();
        weekEnd = thisMonday.atStartOfDay();
        viewer = newUser();
        liker1 = newUser();
        liker2 = newUser();
    }

    @AfterEach
    void tearDown() {
        perspectiveLikeRepository.deleteAllById(likeIds);
        perspectiveRepository.deleteAllById(perspectiveIds);
        userBattleRepository.deleteAllById(userBattleIds);
        battleOptionRepository.deleteAllById(optionIds);
        battleRepository.deleteAllById(battleIds);
        userRepository.deleteAllById(userIds);
    }

    @Test
    @DisplayName("직전 주에 눌린 좋아요 수로 정렬하고, 기간 밖 좋아요와 비공개 대상은 뺀다")
    void ranksByLikesInPreviousWeek() throws Exception {
        Battle battle = battle(BattleStatus.PUBLISHED);
        Perspective second = perspective(battle);
        like(second, liker1, weekStart);
        like(second, liker2, weekEnd.minusDays(8)); // 지지난 주
        Perspective first = perspective(battle);
        like(first, liker1, weekEnd.minusSeconds(1));
        like(first, liker2, weekStart.plusDays(2));
        Perspective onlyThisWeek = perspective(battle);
        like(onlyThisWeek, liker1, weekEnd);
        Perspective hidden = perspective(battle);
        hidden.hide();
        perspectiveRepository.save(hidden);
        like(hidden, liker1, weekStart.plusDays(1));
        Perspective archivedBattle = perspective(battle(BattleStatus.ARCHIVED));
        like(archivedBattle, liker1, weekStart.plusDays(1));

        JsonNode data = fetch(tokenOf(viewer));

        assertThat(data.get("weekStart").asText()).isEqualTo(weekStart.toLocalDate().toString());
        assertThat(data.get("weekEnd").asText()).isEqualTo(weekEnd.toLocalDate().minusDays(1).toString());
        List<JsonNode> items = ownItems(data);
        assertThat(items).extracting(item -> item.get("perspectiveId").asLong())
                .containsExactly(first.getId(), second.getId());
        assertThat(items).extracting(item -> item.get("likeCount").asLong()).containsExactly(2L, 1L);
        assertThat(items.get(0).get("battleId").asLong()).isEqualTo(battle.getId());
        assertThat(items.get(0).get("battleTitle").asText()).isEqualTo(battle.getTitle());
        assertThat(items.get(0).get("content").asText()).isEqualTo("관점 본문");
    }

    @Test
    @DisplayName("사후투표까지 완료한 배틀만 participated 가 true 다")
    void participatedOnlyAfterPostVote() throws Exception {
        Perspective completed = perspective(battle(BattleStatus.PUBLISHED));
        Perspective preVoteOnly = perspective(battle(BattleStatus.PUBLISHED));
        Perspective notJoined = perspective(battle(BattleStatus.PUBLISHED));
        like(completed, liker1, weekStart.plusDays(1));
        like(preVoteOnly, liker1, weekStart.plusDays(1));
        like(notJoined, liker1, weekStart.plusDays(1));
        step(completed.getBattle(), UserBattleStep.COMPLETED);
        step(preVoteOnly.getBattle(), UserBattleStep.TTS_LISTENING);

        List<JsonNode> items = ownItems(fetch(tokenOf(viewer)));

        assertThat(participated(items, completed)).isTrue();
        assertThat(participated(items, preVoteOnly)).isFalse();
        assertThat(participated(items, notJoined)).isFalse();
    }

    @Test
    @DisplayName("토큰 없이도 목록을 보고 participated 는 false 다. 잘못된 토큰은 401 이다")
    void tokenIsOptionalButValidated() throws Exception {
        Perspective perspective = perspective(battle(BattleStatus.PUBLISHED));
        like(perspective, liker1, weekStart.plusDays(1));
        step(perspective.getBattle(), UserBattleStep.COMPLETED);

        List<JsonNode> items = ownItems(fetch(null));

        assertThat(items).hasSize(1);
        assertThat(participated(items, perspective)).isFalse();
        mockMvc.perform(get(URL).header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode fetch(String token) throws Exception {
        var request = get(URL);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data");
    }

    private List<JsonNode> ownItems(JsonNode data) {
        List<JsonNode> items = new ArrayList<>();
        data.get("items").forEach(item -> {
            if (perspectiveIds.contains(item.get("perspectiveId").asLong())) {
                items.add(item);
            }
        });
        return items;
    }

    private boolean participated(List<JsonNode> items, Perspective perspective) {
        return items.stream()
                .filter(item -> item.get("perspectiveId").asLong() == perspective.getId())
                .findFirst().orElseThrow()
                .get("participated").asBoolean();
    }

    private void step(Battle battle, UserBattleStep step) {
        userBattleIds.add(userBattleRepository.save(UserBattle.builder()
                .user(viewer).battle(battle).step(step).build()).getId());
    }

    private void like(Perspective perspective, User user, LocalDateTime createdAt) {
        PerspectiveLike like = perspectiveLikeRepository.save(PerspectiveLike.builder()
                .perspective(perspective).user(user).build());
        likeIds.add(like.getId());
        // JdbcTemplate 은 hibernate.jdbc.time_zone 변환을 거치지 않아 JVM 시간대가 KST 가 아니면 조회 조건과 어긋난다
        transactionTemplate.executeWithoutResult(status -> entityManager
                .createQuery("update PerspectiveLike l set l.createdAt = :createdAt where l.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", like.getId())
                .executeUpdate());
    }

    private Perspective perspective(Battle battle) {
        BattleOption option = battleOptionRepository.findByBattle(battle).get(0);
        Perspective perspective = Perspective.builder()
                .battle(battle).user(liker1).option(option).content("관점 본문").build();
        perspective.publish();
        Perspective saved = perspectiveRepository.save(perspective);
        perspectiveIds.add(saved.getId());
        return saved;
    }

    private Battle battle(BattleStatus status) {
        String title = "주간베스트-" + UUID.randomUUID().toString().substring(0, 8);
        Battle battle = battleRepository.save(Battle.builder()
                .title(title).summary("요약").description("설명").audioDuration(300)
                .status(status).creatorType(BattleCreatorType.ADMIN).build());
        battleIds.add(battle.getId());
        optionIds.add(battleOptionRepository.save(BattleOption.builder()
                .battle(battle).title("찬성").stance("근거").representative("칸트").displayOrder(1).build()).getId());
        return battle;
    }

    private User newUser() {
        User user = userRepository.save(User.builder()
                .userTag("wb-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("주간베스트")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        userIds.add(user.getId());
        return user;
    }

    private String tokenOf(User user) {
        return jwtProvider.createAccessToken(user.getId(), UserRole.USER.name());
    }
}
