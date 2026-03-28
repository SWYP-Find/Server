package com.swyp.app.domain.home.service;

import com.swyp.app.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.app.domain.battle.dto.response.TodayOptionResponse;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.home.dto.response.HomeBattleResponse;
import com.swyp.app.domain.notice.dto.response.NoticeSummaryResponse;
import com.swyp.app.domain.notice.enums.NoticePlacement;
import com.swyp.app.domain.notice.service.NoticeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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
        TodayBattleResponse todayVotePick = battle("today-vote-id", VOTE);
        TodayBattleResponse quizBattle = quiz("quiz-id");
        TodayBattleResponse newBattle = battle("new-id", BATTLE);

        NoticeSummaryResponse notice = new NoticeSummaryResponse(
                generateId(),
                "notice",
                "body",
                null,
                NoticePlacement.HOME_TOP,
                true,
                LocalDateTime.now().minusDays(1),
                null
        );

        when(noticeService.getActiveNotices(NoticePlacement.HOME_TOP, null, 1)).thenReturn(List.of(notice));
        when(battleService.getEditorPicks()).thenReturn(List.of(editorPick));
        when(battleService.getTrendingBattles()).thenReturn(List.of(trendingBattle));
        when(battleService.getBestBattles()).thenReturn(List.of(bestBattle));
        when(battleService.getTodayPicks(VOTE)).thenReturn(List.of(todayVotePick));
        when(battleService.getTodayPicks(QUIZ)).thenReturn(List.of(quizBattle));
        when(battleService.getNewBattles(List.of(
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

        verify(battleService).getNewBattles(argThat(ids -> ids.equals(List.of(
                editorPick.battleId(),
                trendingBattle.battleId(),
                bestBattle.battleId(),
                todayVotePick.battleId(),
                quizBattle.battleId()
        ))));
    }

    @Test
    @DisplayName("데이터가 없으면 false와 빈리스트를 반환한다")
    void getHome_returns_false_and_empty_lists_when_no_data() {
        when(noticeService.getActiveNotices(NoticePlacement.HOME_TOP, null, 1)).thenReturn(List.of());
        when(battleService.getEditorPicks()).thenReturn(List.of());
        when(battleService.getTrendingBattles()).thenReturn(List.of());
        when(battleService.getBestBattles()).thenReturn(List.of());
        when(battleService.getTodayPicks(VOTE)).thenReturn(List.of());
        when(battleService.getTodayPicks(QUIZ)).thenReturn(List.of());
        when(battleService.getNewBattles(List.of())).thenReturn(List.of());

        var response = homeService.getHome();

        assertThat(response.newNotice()).isFalse();
        assertThat(response.editorPicks()).isEmpty();
        assertThat(response.trendingBattles()).isEmpty();
        assertThat(response.bestBattles()).isEmpty();
        assertThat(response.todayPicks()).isEmpty();
        assertThat(response.newBattles()).isEmpty();
    }

    @Test
    @DisplayName("에디터픽만 있을때 제외목록이 정확하다")
    void getHome_excludes_only_editor_pick_ids() {
        TodayBattleResponse editorPick = battle("editor-only", BATTLE);

        when(noticeService.getActiveNotices(NoticePlacement.HOME_TOP, null, 1)).thenReturn(List.of());
        when(battleService.getEditorPicks()).thenReturn(List.of(editorPick));
        when(battleService.getTrendingBattles()).thenReturn(List.of());
        when(battleService.getBestBattles()).thenReturn(List.of());
        when(battleService.getTodayPicks(VOTE)).thenReturn(List.of());
        when(battleService.getTodayPicks(QUIZ)).thenReturn(List.of());
        when(battleService.getNewBattles(List.of(editorPick.battleId()))).thenReturn(List.of());

        homeService.getHome();

        verify(battleService).getNewBattles(List.of(editorPick.battleId()));
    }

    @Test
    @DisplayName("공지가 여러개여도 newNotice는 true이다")
    void getHome_newNotice_true_with_multiple_notices() {
        NoticeSummaryResponse notice1 = new NoticeSummaryResponse(
                generateId(), "notice1", "body1", null,
                NoticePlacement.HOME_TOP, true, LocalDateTime.now().minusDays(1), null
        );

        when(noticeService.getActiveNotices(NoticePlacement.HOME_TOP, null, 1)).thenReturn(List.of(notice1));
        when(battleService.getEditorPicks()).thenReturn(List.of());
        when(battleService.getTrendingBattles()).thenReturn(List.of());
        when(battleService.getBestBattles()).thenReturn(List.of());
        when(battleService.getTodayPicks(VOTE)).thenReturn(List.of());
        when(battleService.getTodayPicks(QUIZ)).thenReturn(List.of());
        when(battleService.getNewBattles(List.of())).thenReturn(List.of());

        var response = homeService.getHome();

        assertThat(response.newNotice()).isTrue();
    }

    private TodayBattleResponse battle(String title, BattleType type) {
        return new TodayBattleResponse(
                generateId(),
                title,
                "summary",
                "thumbnail",
                type,
                10,
                20L,
                90,
                List.of(),
                List.of(
                        new TodayOptionResponse(generateId(), BattleOptionLabel.A, "A", "rep-a", "stance-a", "image-a"),
                        new TodayOptionResponse(generateId(), BattleOptionLabel.B, "B", "rep-b", "stance-b", "image-b")
                )
        );
    }

    private TodayBattleResponse quiz(String title) {
        return new TodayBattleResponse(
                generateId(),
                title,
                "summary",
                "thumbnail",
                QUIZ,
                30,
                40L,
                60,
                List.of(),
                List.of(
                        new TodayOptionResponse(generateId(), BattleOptionLabel.A, "A", "rep-a", "stance-a", "image-a"),
                        new TodayOptionResponse(generateId(), BattleOptionLabel.B, "B", "rep-b", "stance-b", "image-b"),
                        new TodayOptionResponse(generateId(), BattleOptionLabel.C, "C", "rep-c", "stance-c", "image-c"),
                        new TodayOptionResponse(generateId(), BattleOptionLabel.D, "D", "rep-d", "stance-d", "image-d")
                )
        );
    }
}
