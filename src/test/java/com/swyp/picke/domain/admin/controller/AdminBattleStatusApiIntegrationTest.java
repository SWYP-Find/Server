package com.swyp.picke.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
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
 * 어드민 배틀 공개·보관 상태 변경(#622)을 HTTP 로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminBattleStatusApiIntegrationTest {

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
    @DisplayName("공개 배틀을 보관하면 사용자 상세에서 막히고, 다시 공개하면 보인다")
    void archiveThenRepublish() throws Exception {
        Battle battle = battle("보관할 배틀", BattleStatus.PUBLISHED);

        changeStatus(battle.getId(), "ARCHIVED", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        assertThat(battleRepository.findById(battle.getId()).orElseThrow().getStatus()).isEqualTo(BattleStatus.ARCHIVED);
        mockMvc.perform(get("/api/v1/battles/{battleId}", battle.getId()).header("Authorization", "Bearer " + userToken))
                .andExpect(status().is4xxClientError());

        changeStatus(battle.getId(), "PUBLISHED", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        mockMvc.perform(get("/api/v1/battles/{battleId}", battle.getId()).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("같은 상태로 바꾸면 그대로 통과한다")
    void sameStatusIsNoop() throws Exception {
        Battle battle = battle("이미 공개", BattleStatus.PUBLISHED);

        changeStatus(battle.getId(), "PUBLISHED", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("임시저장 배틀 발행·공개 배틀을 임시저장으로·반려 전환은 409, 삭제된 배틀은 404")
    void rejectsOtherTransitions() throws Exception {
        Battle pending = battle("임시저장", BattleStatus.PENDING);
        Battle published = battle("공개", BattleStatus.PUBLISHED);
        Battle deleted = battle("삭제됨", BattleStatus.PUBLISHED);
        deleted.delete();
        battleRepository.save(deleted);

        changeStatus(pending.getId(), "PUBLISHED", adminToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("BATTLE_409_STATUS"));
        changeStatus(published.getId(), "PENDING", adminToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("BATTLE_409_STATUS"));
        changeStatus(published.getId(), "REJECTED", adminToken)
                .andExpect(status().isConflict());
        changeStatus(deleted.getId(), "PUBLISHED", adminToken)
                .andExpect(status().isNotFound());
        assertThat(battleRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(BattleStatus.PENDING);
    }

    @Test
    @DisplayName("일반 사용자는 상태를 바꿀 수 없다")
    void userIsForbidden() throws Exception {
        Battle battle = battle("권한 확인", BattleStatus.PUBLISHED);

        changeStatus(battle.getId(), "ARCHIVED", userToken)
                .andExpect(status().isForbidden());
        assertThat(battleRepository.findById(battle.getId()).orElseThrow().getStatus()).isEqualTo(BattleStatus.PUBLISHED);
    }

    private ResultActions changeStatus(Long battleId, String status, String token) throws Exception {
        return mockMvc.perform(patch("/api/v1/admin/battles/{battleId}/status", battleId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", status))));
    }

    private Battle battle(String title, BattleStatus status) {
        Battle battle = battleRepository.save(Battle.builder()
                .title(title)
                .summary(title + " 요약")
                .description(title + " 설명")
                .audioDuration(300)
                .status(status)
                .creatorType(BattleCreatorType.ADMIN)
                .build());
        battleIds.add(battle.getId());
        battleOptionRepository.save(BattleOption.builder()
                .battle(battle).title("찬성").stance("찬성 근거").representative("칸트").displayOrder(1).build());
        battleOptionRepository.save(BattleOption.builder()
                .battle(battle).title("반대").stance("반대 근거").representative("니체").displayOrder(2).build());
        return battle;
    }

    private User newUser(UserRole role) {
        User user = userRepository.save(User.builder()
                .userTag("st-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("상태테스트")
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
