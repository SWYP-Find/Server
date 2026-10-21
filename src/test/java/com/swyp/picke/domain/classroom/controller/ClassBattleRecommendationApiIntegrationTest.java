package com.swyp.picke.domain.classroom.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.util.ArrayList;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 클래스 Picke 배틀 추천(#551)과 어드민 배틀 클래스 설정을 HTTP 로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassBattleRecommendationApiIntegrationTest {

    private static final String URL = "/api/v1/class-battle-recommendations";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private BattleRepository battleRepository;
    @Autowired private BattleOptionRepository battleOptionRepository;

    private final List<Long> battleIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userToken = tokenOf(newUser(UserRole.USER), UserRole.USER);
        adminToken = tokenOf(newUser(UserRole.ADMIN), UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        battleOptionRepository.deleteAll(battleOptionRepository.findAll().stream()
                .filter(o -> battleIds.contains(o.getBattle().getId())).toList());
        battleRepository.deleteAllById(battleIds);
        userRepository.deleteAllById(userIds);
        battleIds.clear();
        userIds.clear();
    }

    @Test
    @DisplayName("공개·추천 켜짐·수준 일치·카테고리 겹침 배틀만, 참여자 많은 순으로 최대 3개")
    void filtersAndLimitsToThree() throws Exception {
        Battle top = recommendable("정의란 무엇인가", BattleDifficulty.BEGINNER, BattleCategory.PHILOSOPHY, 50);
        Battle second = recommendable("자유의 조건", BattleDifficulty.BEGINNER, BattleCategory.SOCIETY, 40);
        Battle third = recommendable("행복의 기준", BattleDifficulty.BEGINNER, BattleCategory.PHILOSOPHY, 30);
        recommendable("네 번째", BattleDifficulty.BEGINNER, BattleCategory.PHILOSOPHY, 10);
        recommendable("수준이 다름", BattleDifficulty.ADVANCED, BattleCategory.PHILOSOPHY, 99);
        recommendable("카테고리가 다름", BattleDifficulty.BEGINNER, BattleCategory.ART, 99);
        Battle off = battle("추천 꺼짐", BattleStatus.PUBLISHED, BattleCreatorType.ADMIN, 99);
        off.updateClassSettings(BattleDifficulty.BEGINNER, BattleCategory.PHILOSOPHY, false, false);
        battleRepository.save(off);
        Battle pending = battle("비공개", BattleStatus.PENDING, BattleCreatorType.ADMIN, 99);
        pending.updateClassSettings(BattleDifficulty.BEGINNER, BattleCategory.PHILOSOPHY, true, false);
        battleRepository.save(pending);
        Battle deleted = recommendable("삭제됨", BattleDifficulty.BEGINNER, BattleCategory.PHILOSOPHY, 99);
        deleted.delete();
        battleRepository.save(deleted);

        JsonNode items = data(recommend(Map.of("difficulty", "BEGINNER", "categories", "철학,사회"))
                .andExpect(status().isOk())).get("items");

        assertThat(ids(items)).containsExactly(top.getId(), second.getId(), third.getId());
        JsonNode first = items.get(0);
        assertThat(first.get("category").asText()).isEqualTo("철학");
        assertThat(first.get("difficulty").asText()).isEqualTo("BEGINNER");
        assertThat(first.get("audioDuration").asInt()).isEqualTo(300);
        assertThat(first.get("options").get(0).get("position").asText()).isEqualTo("A");
        assertThat(first.get("options").get(0).get("representative").asText()).isEqualTo("칸트");
        assertThat(first.get("options").get(1).get("position").asText()).isEqualTo("B");
    }

    @Test
    @DisplayName("주제를 적으면 단어가 들어간 배틀만, 많이 들어간 순. 적으면 적은 대로, 없으면 빈 목록")
    void keywordFiltersAndReturnsFewer() throws Exception {
        Battle both = recommendable("AI 와 범죄 예측", BattleDifficulty.INTERMEDIATE, BattleCategory.SCIENCE, 1);
        Battle one = recommendable("AI 가 그린 그림은 예술인가", BattleDifficulty.INTERMEDIATE, BattleCategory.SCIENCE, 100);
        recommendable("상관없는 주제", BattleDifficulty.INTERMEDIATE, BattleCategory.SCIENCE, 100);

        JsonNode items = data(recommend(Map.of("difficulty", "INTERMEDIATE", "categories", "SCIENCE", "keyword", "ai, 범죄"))
                .andExpect(status().isOk())).get("items");
        assertThat(ids(items)).containsExactly(both.getId(), one.getId());

        recommend(Map.of("difficulty", "INTERMEDIATE", "categories", "과학", "keyword", "우주"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    @DisplayName("조건 오류는 400 CLASS_400_RECOMMEND_CONDITION")
    void invalidConditions() throws Exception {
        recommend(Map.of("categories", "철학"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_RECOMMEND_CONDITION"));
        recommend(Map.of("difficulty", "BEGINNER"))
                .andExpect(jsonPath("$.error.code").value("CLASS_400_RECOMMEND_CONDITION"));
        recommend(Map.of("difficulty", "BEGINNER", "categories", "철학,사회,문학,예술"))
                .andExpect(jsonPath("$.error.code").value("CLASS_400_RECOMMEND_CONDITION"));
        recommend(Map.of("difficulty", "BEGINNER", "categories", "도덕"))
                .andExpect(jsonPath("$.error.code").value("CLASS_400_RECOMMEND_CONDITION"));
        recommend(Map.of("difficulty", "BEGINNER", "categories", "철학", "keyword", "가".repeat(51)))
                .andExpect(jsonPath("$.error.code").value("CLASS_400_RECOMMEND_CONDITION"));
    }

    @Test
    @DisplayName("어드민 클래스 설정: 수준·카테고리 없이 추천을 켜면 400, 모두 정하면 상세에 반영되고 추천에 나온다")
    void adminClassSettings() throws Exception {
        Battle battle = battle("어드민 설정 배틀", BattleStatus.PUBLISHED, BattleCreatorType.ADMIN, 0);

        updateSettings(battle.getId(), Map.of("classRecommendable", true), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_RECOMMEND_SETTINGS"));

        updateSettings(battle.getId(), Map.of(
                "difficulty", "ADVANCED", "category", "역사", "classRecommendable", true), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.difficulty").value("ADVANCED"))
                .andExpect(jsonPath("$.data.category").value("역사"))
                .andExpect(jsonPath("$.data.classRecommendable").value(true));

        JsonNode items = data(recommend(Map.of("difficulty", "ADVANCED", "categories", "역사"))).get("items");
        assertThat(ids(items)).containsExactly(battle.getId());
    }

    @Test
    @DisplayName("어드민 클래스 설정: 커스텀 배틀은 400, 일반 사용자는 403")
    void adminClassSettingsRejected() throws Exception {
        Battle custom = battle("커스텀", BattleStatus.PENDING, BattleCreatorType.USER, 0);
        updateSettings(custom.getId(), Map.of("classRecommendable", true), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLASS_400_BATTLE"));

        Battle battle = battle("일반", BattleStatus.PUBLISHED, BattleCreatorType.ADMIN, 0);
        updateSettings(battle.getId(), Map.of("difficulty", "BEGINNER"), userToken)
                .andExpect(status().isForbidden());
    }

    // ===== 헬퍼 =====

    private Battle recommendable(String title, BattleDifficulty difficulty, BattleCategory category, int participants) {
        Battle battle = battle(title, BattleStatus.PUBLISHED, BattleCreatorType.ADMIN, participants);
        battle.updateClassSettings(difficulty, category, true, false);
        return battleRepository.save(battle);
    }

    private Battle battle(String title, BattleStatus status, BattleCreatorType creatorType, int participants) {
        Battle battle = Battle.builder()
                .title(title)
                .summary(title + " 요약")
                .description(title + " 설명")
                .audioDuration(300)
                .status(status)
                .creatorType(creatorType)
                .build();
        for (int i = 0; i < participants; i++) {
            battle.addParticipant();
        }
        battle = battleRepository.save(battle);
        battleIds.add(battle.getId());
        battleOptionRepository.save(BattleOption.builder()
                .battle(battle).title("찬성").stance("찬성 근거").representative("칸트").displayOrder(1).build());
        battleOptionRepository.save(BattleOption.builder()
                .battle(battle).title("반대").stance("반대 근거").representative("니체").displayOrder(2).build());
        return battle;
    }

    private ResultActions recommend(Map<String, String> params) throws Exception {
        var request = get(URL).header("Authorization", "Bearer " + userToken);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            request = request.param(entry.getKey(), entry.getValue());
        }
        return mockMvc.perform(request);
    }

    private ResultActions updateSettings(Long battleId, Map<String, Object> body, String token) throws Exception {
        return mockMvc.perform(patch("/api/v1/admin/battles/{battleId}/class-settings", battleId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private JsonNode data(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("data");
    }

    private static List<Long> ids(JsonNode items) {
        List<Long> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.get("battleId").asLong()));
        return ids;
    }

    private User newUser(UserRole role) {
        User user = userRepository.save(User.builder()
                .userTag("rec-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("추천테스트")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
        userIds.add(user.getId());
        return user;
    }

    private String tokenOf(User user, UserRole role) {
        return jwtProvider.createAccessToken(user.getId(), role.name());
    }
}
