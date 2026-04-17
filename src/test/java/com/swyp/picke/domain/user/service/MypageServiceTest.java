package com.swyp.picke.domain.user.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.service.BattleQueryService;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.entity.PerspectiveLike;
import com.swyp.picke.domain.perspective.service.PerspectiveQueryService;
import com.swyp.picke.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.swyp.picke.domain.user.dto.response.BattleRecordListResponse;
import com.swyp.picke.domain.user.dto.response.ContentActivityListResponse;
import com.swyp.picke.domain.user.dto.response.CreditHistoryListResponse;
import com.swyp.picke.domain.user.dto.response.MypageResponse;
import com.swyp.picke.domain.user.dto.response.NotificationSettingsResponse;
import com.swyp.picke.domain.user.dto.response.RecapResponse;
import com.swyp.picke.domain.user.dto.response.UserSummary;
import com.swyp.picke.domain.user.entity.CreditHistory;
import com.swyp.picke.domain.user.enums.ActivityType;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import com.swyp.picke.domain.user.enums.TierCode;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserProfile;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.entity.UserSettings;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.enums.VoteSide;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.service.VoteQueryService;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MypageServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private CreditService creditService;
    @Mock
    private VoteQueryService voteQueryService;
    @Mock
    private BattleQueryService battleQueryService;
    @Mock
    private PerspectiveQueryService perspectiveQueryService;
    @Mock
    private ResourceUrlProvider resourceUrlProvider;

    @InjectMocks
    private MypageService mypageService;

    private final AtomicLong idGenerator = new AtomicLong(100L);

    private Long generateId() {
        return idGenerator.getAndIncrement();
    }

    @Test
    @DisplayName("프로필, 철학자, 티어 정보를 반환한다")
    void getMypage_returns_profile_philosopher_tier() {
        User user = createUser(1L, "myTag");
        UserProfile profile = createProfile(user, "nick", CharacterType.OWL);
        profile.updatePhilosopherType(PhilosopherType.KANT);

        when(userService.findCurrentUser()).thenReturn(user);
        when(userService.findUserProfile(1L)).thenReturn(profile);
        when(creditService.getTotalPoints(1L)).thenReturn(0);
        when(resourceUrlProvider.getImageUrl(any(), anyString())).thenReturn("http://localhost:8080/api/v1/resources/images/CHARACTER/owl.png");

        MypageResponse response = mypageService.getMypage();

        assertThat(response.profile().userTag()).isEqualTo("myTag");
        assertThat(response.profile().nickname()).isEqualTo("nick");
        assertThat(response.profile().characterType()).isEqualTo(CharacterType.OWL);
        assertThat(response.profile().mannerTemperature()).isEqualByComparingTo(BigDecimal.valueOf(36.5));
        assertThat(response.philosopher().philosopherType()).isEqualTo(PhilosopherType.KANT);
        assertThat(response.philosopher().typeName()).isEqualTo("원칙형");
        assertThat(response.philosopher().description()).isNotNull();
        assertThat(response.tier().tierCode()).isEqualTo(TierCode.WANDERER);
        assertThat(response.tier().currentPoint()).isZero();
    }

    @Test
    @DisplayName("철학자카드와 성향점수와 선호보고서를 반환한다")
    void getRecap_returns_cards_scores_report() {
        User user = createUser(1L, "tag");
        UserProfile profile = createProfile(user, "nick", CharacterType.OWL);
        profile.updatePhilosopherType(PhilosopherType.KANT);

        when(userService.findCurrentUser()).thenReturn(user);
        when(userService.findUserProfile(1L)).thenReturn(profile);
        when(resourceUrlProvider.getImageUrl(any(), anyString())).thenReturn("http://localhost:8080/api/v1/resources/images/PHILOSOPHER/kant.png");
        when(voteQueryService.countTotalParticipation(1L)).thenReturn(15L);
        when(voteQueryService.countOpinionChanges(1L)).thenReturn(3L);
        when(voteQueryService.calculateBattleWinRate(1L)).thenReturn(70);

        List<Long> battleIds = List.of(generateId());
        when(voteQueryService.findParticipatedBattleIds(1L)).thenReturn(battleIds);

        LinkedHashMap<String, Long> topTags = new LinkedHashMap<>();
        topTags.put("정치", 5L);
        topTags.put("경제", 3L);
        when(battleQueryService.getTopTagsByBattleIds(battleIds, 4)).thenReturn(topTags);

        RecapResponse response = mypageService.getRecap();

        assertThat(response.myCard().philosopherType()).isEqualTo(PhilosopherType.KANT);
        assertThat(response.myCard().keywordTags()).containsExactly("#원칙", "#의무", "#윤리", "#절제");
        assertThat(response.bestMatchCard().philosopherType()).isEqualTo(PhilosopherType.CONFUCIUS);
        assertThat(response.worstMatchCard().philosopherType()).isEqualTo(PhilosopherType.NIETZSCHE);
        assertThat(response.scores().principle()).isEqualTo(92);
        assertThat(response.scores().ideal()).isEqualTo(45);
        assertThat(response.preferenceReport().totalParticipation()).isEqualTo(15);
        assertThat(response.preferenceReport().opinionChanges()).isEqualTo(3);
        assertThat(response.preferenceReport().battleWinRate()).isEqualTo(70);
        assertThat(response.preferenceReport().favoriteTopics()).hasSize(2);
        assertThat(response.preferenceReport().favoriteTopics().get(0).tagName()).isEqualTo("정치");
    }

    @Test
    @DisplayName("철학자유형이 미산출이면 recap은 null이다")
    void getRecap_returns_null_when_no_philosopher() {
        User user = createUser(1L, "tag");
        UserProfile profile = createProfile(user, "nick", CharacterType.OWL);

        when(userService.findCurrentUser()).thenReturn(user);
        when(userService.findUserProfile(1L)).thenReturn(profile);

        RecapResponse response = mypageService.getRecap();

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("투표기록을 페이지네이션하여 반환한다")
    void getBattleRecords_returns_paginated_records() {
        User user = createUser(1L, "tag");
        Battle battle = createBattle("배틀 제목");
        BattleOption optionA = createOption(battle, BattleOptionLabel.A);
        BattleVote vote = BattleVote.builder()
                .user(user)
                .battle(battle)
                .preVoteOption(optionA)
                .build();
        ReflectionTestUtils.setField(vote, "id", generateId());
        ReflectionTestUtils.setField(vote, "createdAt", LocalDateTime.now());

        when(userService.findCurrentUser()).thenReturn(user);
        when(voteQueryService.findUserVotes(1L, 0, 2, null)).thenReturn(List.of(vote));
        when(voteQueryService.countUserVotes(1L, null)).thenReturn(5L);

        BattleRecordListResponse response = mypageService.getBattleRecords(0, 2, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).voteSide()).isEqualTo(VoteSide.PRO);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextOffset()).isEqualTo(2);
    }

    @Test
    @DisplayName("다음페이지가 없으면 hasNext가 false이다")
    void getBattleRecords_returns_no_next_when_last_page() {
        User user = createUser(1L, "tag");
        Battle battle = createBattle("제목");
        BattleOption optionA = createOption(battle, BattleOptionLabel.A);
        BattleVote vote = BattleVote.builder()
                .user(user)
                .battle(battle)
                .preVoteOption(optionA)
                .build();
        ReflectionTestUtils.setField(vote, "id", generateId());
        ReflectionTestUtils.setField(vote, "createdAt", LocalDateTime.now());

        when(userService.findCurrentUser()).thenReturn(user);
        when(voteQueryService.findUserVotes(1L, 0, 20, null)).thenReturn(List.of(vote));
        when(voteQueryService.countUserVotes(1L, null)).thenReturn(1L);

        BattleRecordListResponse response = mypageService.getBattleRecords(null, null, null);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextOffset()).isNull();
    }

    @Test
    @DisplayName("voteSide 필터가 적용된다")
    void getBattleRecords_applies_vote_side_filter() {
        User user = createUser(1L, "tag");

        when(userService.findCurrentUser()).thenReturn(user);
        when(voteQueryService.findUserVotes(1L, 0, 20, BattleOptionLabel.A)).thenReturn(List.of());
        when(voteQueryService.countUserVotes(1L, BattleOptionLabel.A)).thenReturn(0L);

        mypageService.getBattleRecords(null, null, VoteSide.PRO);

        verify(voteQueryService).findUserVotes(eq(1L), eq(0), eq(20), eq(BattleOptionLabel.A));
    }

    @Test
    @DisplayName("COMMENT 타입으로 댓글활동을 반환한다")
    void getContentActivities_returns_comments() {
        User user = createUser(1L, "tag");
        UserProfile profile = createProfile(user, "nick", CharacterType.OWL);
        Battle battle = createBattle("배틀");
        Long battleId = battle.getId();
        BattleOption option = createOption(battle, BattleOptionLabel.A);
        Long optionId = option.getId();

        Perspective perspective = Perspective.builder()
                .battle(battle)
                .user(user)
                .option(option)
                .content("관점 내용")
                .build();
        ReflectionTestUtils.setField(perspective, "id", generateId());

        PerspectiveComment comment = PerspectiveComment.builder()
                .perspective(perspective)
                .user(user)
                .content("댓글")
                .build();
        ReflectionTestUtils.setField(comment, "id", generateId());
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());

        when(userService.findCurrentUser()).thenReturn(user);
        when(userService.findUserProfile(1L)).thenReturn(profile);
        when(perspectiveQueryService.findUserComments(1L, 0, 20)).thenReturn(List.of(comment));
        when(perspectiveQueryService.countUserComments(1L)).thenReturn(1L);
        when(battleQueryService.findBattlesByIds(List.of(battleId))).thenReturn(Map.of(battleId, battle));
        when(battleQueryService.findOptionsByIds(List.of(optionId))).thenReturn(Map.of(optionId, option));
        when(userService.findSummaryById(1L)).thenReturn(new UserSummary("tag", "nick", "OWL"));

        ContentActivityListResponse response = mypageService.getContentActivities(null, null, ActivityType.COMMENT);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).activityType()).isEqualTo(ActivityType.COMMENT);
        assertThat(response.items().get(0).content()).isEqualTo("댓글");
    }

    @Test
    @DisplayName("LIKE 타입으로 좋아요활동을 반환한다")
    void getContentActivities_returns_likes() {
        User user = createUser(1L, "tag");
        Battle battle = createBattle("배틀");
        Long battleId = battle.getId();
        BattleOption option = createOption(battle, BattleOptionLabel.B);
        Long optionId = option.getId();

        Perspective perspective = Perspective.builder()
                .battle(battle)
                .user(user)
                .option(option)
                .content("관점 내용")
                .build();
        ReflectionTestUtils.setField(perspective, "id", generateId());

        PerspectiveLike like = PerspectiveLike.builder()
                .perspective(perspective)
                .user(user)
                .build();
        ReflectionTestUtils.setField(like, "id", generateId());
        ReflectionTestUtils.setField(like, "createdAt", LocalDateTime.now());

        when(userService.findCurrentUser()).thenReturn(user);
        when(perspectiveQueryService.findUserLikes(1L, 0, 20)).thenReturn(List.of(like));
        when(perspectiveQueryService.countUserLikes(1L)).thenReturn(1L);
        when(battleQueryService.findBattlesByIds(List.of(battleId))).thenReturn(Map.of(battleId, battle));
        when(battleQueryService.findOptionsByIds(List.of(optionId))).thenReturn(Map.of(optionId, option));
        when(userService.findSummaryById(1L)).thenReturn(new UserSummary("tag", "nick", "OWL"));

        ContentActivityListResponse response = mypageService.getContentActivities(null, null, ActivityType.LIKE);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).activityType()).isEqualTo(ActivityType.LIKE);
    }

    @Test
    @DisplayName("크레딧 내역을 최신순으로 offset 페이징 변환해 반환한다")
    void getCreditHistory_returns_paginated_history() {
        User user = createUser(1L, "tag");
        CreditHistory latest = creditHistory(301L, user, CreditType.BEST_COMMENT, 50, 91L, LocalDateTime.now());
        CreditHistory older = creditHistory(300L, user, CreditType.BATTLE_VOTE, 5, 90L, LocalDateTime.now().minusDays(1));

        when(userService.findCurrentUser()).thenReturn(user);
        when(creditService.getHistory(1L, PageRequest.of(0, 2)))
                .thenReturn(new PageImpl<>(List.of(latest, older), PageRequest.of(0, 2), 3));

        CreditHistoryListResponse response = mypageService.getCreditHistory(0, 2);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).id()).isEqualTo(301L);
        assertThat(response.items().get(0).creditType()).isEqualTo(CreditType.BEST_COMMENT);
        assertThat(response.items().get(1).id()).isEqualTo(300L);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextOffset()).isEqualTo(2);
    }

    @Test
    @DisplayName("알림설정을 반환한다")
    void getNotificationSettings_returns_settings() {
        User user = createUser(1L, "tag");
        UserSettings settings = UserSettings.builder()
                .user(user)
                .newBattleEnabled(true)
                .battleResultEnabled(false)
                .commentReplyEnabled(true)
                .newCommentEnabled(true)
                .contentLikeEnabled(false)
                .marketingEventEnabled(false)
                .build();

        when(userService.findCurrentUser()).thenReturn(user);
        when(userService.findUserSettings(1L)).thenReturn(settings);

        NotificationSettingsResponse response = mypageService.getNotificationSettings();

        assertThat(response.newBattleEnabled()).isTrue();
        assertThat(response.battleResultEnabled()).isFalse();
        assertThat(response.commentReplyEnabled()).isTrue();
        assertThat(response.newCommentEnabled()).isTrue();
        assertThat(response.contentLikeEnabled()).isFalse();
        assertThat(response.marketingEventEnabled()).isFalse();
    }

    @Test
    @DisplayName("설정을 업데이트하고 반환한다")
    void updateNotificationSettings_updates_and_returns() {
        User user = createUser(1L, "tag");
        UserSettings settings = UserSettings.builder()
                .user(user)
                .newBattleEnabled(false)
                .battleResultEnabled(false)
                .commentReplyEnabled(false)
                .newCommentEnabled(false)
                .contentLikeEnabled(false)
                .marketingEventEnabled(false)
                .build();

        when(userService.findCurrentUser()).thenReturn(user);
        when(userService.findUserSettings(1L)).thenReturn(settings);

        UpdateNotificationSettingsRequest request = new UpdateNotificationSettingsRequest(
                true, null, true, null, null, true
        );

        NotificationSettingsResponse response = mypageService.updateNotificationSettings(request);

        assertThat(response.newBattleEnabled()).isTrue();
        assertThat(response.battleResultEnabled()).isFalse();
        assertThat(response.commentReplyEnabled()).isTrue();
        assertThat(response.marketingEventEnabled()).isTrue();
    }

    private User createUser(Long id, String userTag) {
        User user = User.builder()
                .userTag(userTag)
                .nickname("nickname")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserProfile createProfile(User user, String nickname, CharacterType characterType) {
        return UserProfile.builder()
                .user(user)
                .nickname(nickname)
                .characterType(characterType)
                .mannerTemperature(BigDecimal.valueOf(36.5))
                .build();
    }

    private Battle createBattle(String title) {
        Battle battle = Battle.builder()
                .title(title)
                .summary("summary")
                .status(BattleStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(battle, "id", generateId());
        return battle;
    }

    private BattleOption createOption(Battle battle, BattleOptionLabel label) {
        BattleOption option = BattleOption.builder()
                .battle(battle)
                .label(label)
                .title(label.name())
                .stance("stance-" + label.name())
                .build();
        ReflectionTestUtils.setField(option, "id", generateId());
        return option;
    }

    private CreditHistory creditHistory(
            Long id,
            User user,
            CreditType creditType,
            int amount,
            Long referenceId,
            LocalDateTime createdAt
    ) {
        CreditHistory history = CreditHistory.builder()
                .user(user)
                .creditType(creditType)
                .amount(amount)
                .referenceId(referenceId)
                .build();
        ReflectionTestUtils.setField(history, "id", id);
        ReflectionTestUtils.setField(history, "createdAt", createdAt);
        return history;
    }
}
