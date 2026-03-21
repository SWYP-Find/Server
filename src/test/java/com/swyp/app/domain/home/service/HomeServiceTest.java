package com.swyp.app.domain.home.service;

import com.swyp.app.domain.battle.dto.response.BattleOptionResponse;
import com.swyp.app.domain.battle.dto.response.BattleSummaryResponse;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.home.dto.response.HomeBattleResponse;
import com.swyp.app.domain.notice.dto.response.NoticeSummaryResponse;
import com.swyp.app.domain.notice.entity.NoticePlacement;
import com.swyp.app.domain.notice.service.NoticeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.swyp.app.domain.battle.enums.BattleType.BATTLE;
import static com.swyp.app.domain.battle.enums.BattleType.QUIZ;
import static com.swyp.app.domain.battle.enums.BattleType.VOTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private BattleService battleService;
    @Mock
    private NoticeService noticeService;

    @InjectMocks
    private HomeService homeService;

    @Test
    void getHome_명세기준으로_섹션별_데이터를_조합한다() {
        BattleSummaryResponse editorPick = battle("editor-id", BATTLE);
        BattleSummaryResponse trendingBattle = battle("trending-id", BATTLE);
        BattleSummaryResponse bestBattle = battle("best-id", BATTLE);
        BattleSummaryResponse todayVotePick = battle("today-vote-id", VOTE);
        BattleSummaryResponse quizBattle = quiz("quiz-id");
        BattleSummaryResponse newBattle = battle("new-id", BATTLE);

        NoticeSummaryResponse notice = new NoticeSummaryResponse(
                UUID.randomUUID(),
                "notice",
                "body",
                null,
                NoticePlacement.HOME_TOP,
                true,
                LocalDateTime.now().minusDays(1),
                null
        );

        when(noticeService.getActiveNotices(NoticePlacement.HOME_TOP, null, 1)).thenReturn(List.of(notice));
        when(battleService.getHomeEditorPicks()).thenReturn(List.of(editorPick));
        when(battleService.getHomeTrendingBattles()).thenReturn(List.of(trendingBattle));
        when(battleService.getHomeBestBattles()).thenReturn(List.of(bestBattle));
        when(battleService.getHomeTodayPicks(VOTE)).thenReturn(List.of(todayVotePick));
        when(battleService.getHomeTodayPicks(QUIZ)).thenReturn(List.of(quizBattle));
        when(battleService.getHomeNewBattles(List.of(
                editorPick.battleId(),
                trendingBattle.battleId(),
                bestBattle.battleId(),
                todayVotePick.battleId(),
                quizBattle.battleId()
        ))).thenReturn(List.of(newBattle));

        var response = homeService.getHome();

        assertThat(response.newNotice()).isTrue();
        assertThat(response.editorPicks()).extracting(HomeBattleResponse::title).containsExactly("editor-id");
        assertThat(response.trendingBattles()).extracting(HomeBattleResponse::title).containsExactly("trending-id");
        assertThat(response.bestBattles()).extracting(HomeBattleResponse::title).containsExactly("best-id");
        assertThat(response.todayPicks()).extracting(HomeBattleResponse::title).containsExactly("today-vote-id", "quiz-id");
        assertThat(response.newBattles()).extracting(HomeBattleResponse::title).containsExactly("new-id");
        assertThat(response.todayPicks().get(0).options()).extracting(option -> option.text()).containsExactly("A", "B");
        assertThat(response.todayPicks().get(1).options()).extracting(option -> option.text()).containsExactly("A", "B", "C", "D");

        verify(battleService).getHomeNewBattles(argThat(ids -> ids.equals(List.of(
                editorPick.battleId(),
                trendingBattle.battleId(),
                bestBattle.battleId(),
                todayVotePick.battleId(),
                quizBattle.battleId()
        ))));
    }

    @Test
    void getHome_데이터가_없으면_false와_빈리스트를_반환한다() {
        when(noticeService.getActiveNotices(NoticePlacement.HOME_TOP, null, 1)).thenReturn(List.of());
        when(battleService.getHomeEditorPicks()).thenReturn(List.of());
        when(battleService.getHomeTrendingBattles()).thenReturn(List.of());
        when(battleService.getHomeBestBattles()).thenReturn(List.of());
        when(battleService.getHomeTodayPicks(VOTE)).thenReturn(List.of());
        when(battleService.getHomeTodayPicks(QUIZ)).thenReturn(List.of());
        when(battleService.getHomeNewBattles(List.of())).thenReturn(List.of());

        var response = homeService.getHome();

        assertThat(response.newNotice()).isFalse();
        assertThat(response.editorPicks()).isEmpty();
        assertThat(response.trendingBattles()).isEmpty();
        assertThat(response.bestBattles()).isEmpty();
        assertThat(response.todayPicks()).isEmpty();
        assertThat(response.newBattles()).isEmpty();
    }

    private BattleSummaryResponse battle(String title, BattleType type) {
        return new BattleSummaryResponse(
                UUID.randomUUID(),
                title,
                "summary",
                "thumbnail",
                type,
                10,
                20L,
                90,
                List.of(),
                List.of(
                        new BattleOptionResponse(UUID.randomUUID(), BattleOptionLabel.A, "A", "stance-a", "rep-a", "quote-a", "image-a", List.of()),
                        new BattleOptionResponse(UUID.randomUUID(), BattleOptionLabel.B, "B", "stance-b", "rep-b", "quote-b", "image-b", List.of())
                )
        );
    }

    private BattleSummaryResponse quiz(String title) {
        return new BattleSummaryResponse(
                UUID.randomUUID(),
                title,
                "summary",
                "thumbnail",
                QUIZ,
                30,
                40L,
                60,
                List.of(),
                List.of(
                        new BattleOptionResponse(UUID.randomUUID(), BattleOptionLabel.A, "A", "stance-a", "rep-a", "quote-a", "image-a", List.of()),
                        new BattleOptionResponse(UUID.randomUUID(), BattleOptionLabel.B, "B", "stance-b", "rep-b", "quote-b", "image-b", List.of()),
                        new BattleOptionResponse(UUID.randomUUID(), BattleOptionLabel.C, "C", "stance-c", "rep-c", "quote-c", "image-c", List.of()),
                        new BattleOptionResponse(UUID.randomUUID(), BattleOptionLabel.D, "D", "stance-d", "rep-d", "quote-d", "image-d", List.of())
                )
        );
    }
}
