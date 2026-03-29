package com.swyp.picke.domain.home.service;

import com.swyp.picke.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.picke.domain.battle.dto.response.TodayOptionResponse;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleType;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.home.dto.response.*;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.service.NotificationService;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.swyp.picke.domain.battle.enums.BattleType.BATTLE;
import static com.swyp.picke.domain.battle.enums.BattleType.QUIZ;
import static com.swyp.picke.domain.battle.enums.BattleType.VOTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private BattleService battleService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private HomeService homeService;

    private final AtomicLong idGenerator = new AtomicLong(1L);

    private Long generateId() {
        return idGenerator.getAndIncrement();
    }

    @Test
    @DisplayName("명세기준으로 섹션별 데이터를 조합한다")
    void getHome_aggregates_sections_by_spec() {
        TodayBattleResponse editorPick = battle("editor-id", BATTLE);
        TodayBattleResponse trendingBattle = battle("trending-id", BATTLE);
        TodayBattleResponse bestBattle = battle("best-id", BATTLE);
        TodayBattleResponse todayVote = vote("vote-id");
        TodayBattleResponse todayQuiz = quiz("quiz-id");
        TodayBattleResponse newBattle = battle("new-id", BATTLE);

        when(notificationService.hasNewBroadcast(NotificationCategory.NOTICE)).thenReturn(true);
        when(battleService.getEditorPicks(10)).thenReturn(List.of(editorPick));
        when(battleService.getTrendingBattles(4)).thenReturn(List.of(trendingBattle));
        when(battleService.getBestBattles(3)).thenReturn(List.of(bestBattle));
        when(battleService.getTodayPicks(VOTE, 1)).thenReturn(List.of(todayVote));
        when(battleService.getTodayPicks(QUIZ, 1)).thenReturn(List.of(todayQuiz));

        when(battleService.getNewBattles(List.of(
                editorPick.battleId(),
                trendingBattle.battleId(),
                bestBattle.battleId(),
                todayVote.battleId(),
                todayQuiz.battleId()
        ), 3)).thenReturn(List.of(newBattle));

        var response = homeService.getHome();

        assertThat(response.newNotice()).isTrue();
        assertThat(response.editorPicks()).extracting(HomeEditorPickResponse::title).containsExactly("editor-id");
        assertThat(response.trendingBattles()).extracting(HomeTrendingResponse::title).containsExactly("trending-id");
        assertThat(response.bestBattles()).extracting(HomeBestBattleResponse::title).containsExactly("best-id");
        assertThat(response.todayQuizzes()).extracting(HomeTodayQuizResponse::title).containsExactly("quiz-id");
        assertThat(response.todayVotes()).hasSize(1);
        assertThat(response.todayVotes().get(0).titlePrefix()).isEqualTo("도덕의 기준은");
        assertThat(response.todayVotes().get(0).options()).extracting(HomeTodayVoteOptionResponse::title)
                .containsExactly("결과", "의도", "규칙", "덕");
        assertThat(response.todayQuizzes().get(0).itemA()).isEqualTo("정답");
        assertThat(response.newBattles()).extracting(HomeNewBattleResponse::title).containsExactly("new-id");

        verify(battleService).getNewBattles(argThat(ids -> ids.equals(List.of(
                editorPick.battleId(),
                trendingBattle.battleId(),
                bestBattle.battleId(),
                todayVote.battleId(),
                todayQuiz.battleId()
        ))), eq(3));
    }

    @Test
    @DisplayName("데이터가 없으면 false와 빈리스트를 반환한다")
    void getHome_returns_false_and_empty_lists_when_no_data() {
        when(notificationService.hasNewBroadcast(NotificationCategory.NOTICE)).thenReturn(false);
        when(battleService.getEditorPicks(10)).thenReturn(List.of());
        when(battleService.getTrendingBattles(4)).thenReturn(List.of());
        when(battleService.getBestBattles(3)).thenReturn(List.of());
        when(battleService.getTodayPicks(VOTE, 1)).thenReturn(List.of());
        when(battleService.getTodayPicks(QUIZ, 1)).thenReturn(List.of());
        when(battleService.getNewBattles(List.of(), 3)).thenReturn(List.of());

        var response = homeService.getHome();

        assertThat(response.newNotice()).isFalse();
        assertThat(response.editorPicks()).isEmpty();
        assertThat(response.trendingBattles()).isEmpty();
        assertThat(response.bestBattles()).isEmpty();
        assertThat(response.todayQuizzes()).isEmpty();
        assertThat(response.todayVotes()).isEmpty();
        assertThat(response.newBattles()).isEmpty();
    }

    @Test
    @DisplayName("에디터픽만 있을때 제외목록이 정확하다")
    void getHome_excludes_only_editor_pick_ids() {
        TodayBattleResponse editorPick = battle("editor-only", BATTLE);

        when(notificationService.hasNewBroadcast(NotificationCategory.NOTICE)).thenReturn(false);
        when(battleService.getEditorPicks(10)).thenReturn(List.of(editorPick));
        when(battleService.getTrendingBattles(4)).thenReturn(List.of());
        when(battleService.getBestBattles(3)).thenReturn(List.of());
        when(battleService.getTodayPicks(VOTE, 1)).thenReturn(List.of());
        when(battleService.getTodayPicks(QUIZ, 1)).thenReturn(List.of());
        when(battleService.getNewBattles(List.of(editorPick.battleId()), 3)).thenReturn(List.of());

        homeService.getHome();

        verify(battleService).getNewBattles(List.of(editorPick.battleId()), 3);
    }

    @Test
    @DisplayName("공지 브로드캐스트가 있으면 newNotice는 true이다")
    void getHome_newNotice_true_with_broadcast() {
        when(notificationService.hasNewBroadcast(NotificationCategory.NOTICE)).thenReturn(true);
        when(battleService.getEditorPicks(10)).thenReturn(List.of());
        when(battleService.getTrendingBattles(4)).thenReturn(List.of());
        when(battleService.getBestBattles(3)).thenReturn(List.of());
        when(battleService.getTodayPicks(VOTE, 1)).thenReturn(List.of());
        when(battleService.getTodayPicks(QUIZ, 1)).thenReturn(List.of());
        when(battleService.getNewBattles(List.of(), 3)).thenReturn(List.of());

        var response = homeService.getHome();

        assertThat(response.newNotice()).isTrue();
    }

    private TodayBattleResponse battle(String title, BattleType type) {
        return new TodayBattleResponse(
                generateId(), title, "summary", "thumbnail", type,
                10, 20L, 90,
                List.of(),
                List.of(
                        new TodayOptionResponse(generateId(), BattleOptionLabel.A, "A", "rep-a", "stance-a", "image-a"),
                        new TodayOptionResponse(generateId(), BattleOptionLabel.B, "B", "rep-b", "stance-b", "image-b")
                ),
                null, null, null, null, null, null
        );
    }

    private TodayBattleResponse quiz(String title) {
        return new TodayBattleResponse(
                generateId(), title, "summary", "thumbnail", QUIZ,
                30, 40L, 60,
                List.of(),
                List.of(),
                null, null, "정답", "정답 설명", "오답", "오답 설명"
        );
    }

    private TodayBattleResponse vote(String title) {
        return new TodayBattleResponse(
                generateId(), title, "summary", "thumbnail", VOTE,
                50, 60L, 0,
                List.of(),
                List.of(
                        new TodayOptionResponse(generateId(), BattleOptionLabel.A, "결과", null, null, null),
                        new TodayOptionResponse(generateId(), BattleOptionLabel.B, "의도", null, null, null),
                        new TodayOptionResponse(generateId(), BattleOptionLabel.C, "규칙", null, null, null),
                        new TodayOptionResponse(generateId(), BattleOptionLabel.D, "덕", null, null, null)
                ),
                "도덕의 기준은", "이다", null, null, null, null
        );
    }
}