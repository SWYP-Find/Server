package com.swyp.picke.domain.home.service;

import com.swyp.picke.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.picke.domain.battle.dto.response.TodayOptionResponse;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.home.dto.response.HomeTodayQuizResponse;
import com.swyp.picke.domain.home.dto.response.HomeTodayVoteOptionResponse;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.service.NotificationService;
import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.entity.PollOption;
import com.swyp.picke.domain.poll.enums.PollOptionLabel;
import com.swyp.picke.domain.poll.enums.PollStatus;
import com.swyp.picke.domain.poll.service.PollService;
import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizOption;
import com.swyp.picke.domain.quiz.enums.QuizOptionLabel;
import com.swyp.picke.domain.quiz.enums.QuizStatus;
import com.swyp.picke.domain.quiz.service.QuizService;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private BattleService battleService;

    @Mock
    private QuizService quizService;

    @Mock
    private PollService pollService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private HomeService homeService;

    @Test
    @DisplayName("홈 응답에 배틀/퀴즈/투표 섹션을 조합해 반환한다")
    void getHome_aggregates_sections() {
        Long userId = 1L;

        TodayBattleResponse editorPick = battle(101L, "editor-id");
        TodayBattleResponse trendingBattle = battle(102L, "trending-id");
        TodayBattleResponse bestBattle = battle(103L, "best-id");
        TodayBattleResponse newBattle = battle(104L, "new-id");

        Quiz quiz = Quiz.builder()
                .title("오늘의 퀴즈")
                .targetDate(LocalDate.now())
                .status(QuizStatus.PUBLISHED)
                .build();
        QuizOption quizA = QuizOption.builder()
                .quiz(quiz)
                .label(QuizOptionLabel.A)
                .text("정답")
                .detailText("정답 설명")
                .isCorrect(true)
                .displayOrder(1)
                .build();
        QuizOption quizB = QuizOption.builder()
                .quiz(quiz)
                .label(QuizOptionLabel.B)
                .text("오답")
                .detailText("오답 설명")
                .isCorrect(false)
                .displayOrder(2)
                .build();

        Poll poll = Poll.builder()
                .titlePrefix("찬성 vs 반대")
                .titleSuffix("당신의 선택은?")
                .targetDate(LocalDate.now())
                .status(PollStatus.PUBLISHED)
                .build();
        PollOption pollB = PollOption.builder()
                .poll(poll)
                .label(PollOptionLabel.B)
                .title("반대")
                .displayOrder(2)
                .voteCount(3L)
                .build();
        PollOption pollA = PollOption.builder()
                .poll(poll)
                .label(PollOptionLabel.A)
                .title("찬성")
                .displayOrder(1)
                .voteCount(7L)
                .build();

        when(notificationService.hasNewBroadcast(userId, NotificationCategory.NOTICE)).thenReturn(true);
        when(battleService.getEditorPicks()).thenReturn(List.of(editorPick));
        when(battleService.getTrendingBattles()).thenReturn(List.of(trendingBattle));
        when(battleService.getBestBattles()).thenReturn(List.of(bestBattle));
        when(quizService.getTodayPicks(1)).thenReturn(List.of(quiz));
        when(quizService.getOptions(quiz)).thenReturn(List.of(quizA, quizB));
        when(quizService.countVotes(quiz)).thenReturn(12L);
        when(pollService.getTodayPicks(1)).thenReturn(List.of(poll));
        when(pollService.getOptions(poll)).thenReturn(List.of(pollB, pollA));
        when(pollService.countVotes(poll)).thenReturn(10L);

        when(battleService.getNewBattles(List.of(
                editorPick.battleId(),
                trendingBattle.battleId(),
                bestBattle.battleId()
        ))).thenReturn(List.of(newBattle));

        var response = homeService.getHome(userId);

        assertThat(response.newNotice()).isTrue();
        assertThat(response.editorPicks()).hasSize(1);
        assertThat(response.trendingBattles()).hasSize(1);
        assertThat(response.bestBattles()).hasSize(1);
        assertThat(response.newBattles()).hasSize(1);

        assertThat(response.todayQuizzes()).hasSize(1);
        HomeTodayQuizResponse quizResponse = response.todayQuizzes().getFirst();
        assertThat(quizResponse.title()).isEqualTo("오늘의 퀴즈");
        assertThat(quizResponse.summary()).isEqualTo("왼쪽과 오른쪽 중 정답을 선택하세요");
        assertThat(quizResponse.itemA()).isEqualTo("정답");
        assertThat(quizResponse.itemADesc()).isEqualTo("정답 설명");
        assertThat(quizResponse.itemB()).isEqualTo("오답");
        assertThat(quizResponse.participantsCount()).isEqualTo(12L);

        assertThat(response.todayVotes()).hasSize(1);
        assertThat(response.todayVotes().getFirst().titlePrefix()).isEqualTo("찬성 vs 반대");
        assertThat(response.todayVotes().getFirst().summary()).isEqualTo("빈칸에 들어갈 가장 적절한 답을 골라주세요");
        assertThat(response.todayVotes().getFirst().participantsCount()).isEqualTo(10L);
        assertThat(response.todayVotes().getFirst().options())
                .extracting(HomeTodayVoteOptionResponse::label, HomeTodayVoteOptionResponse::title)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(BattleOptionLabel.A, "찬성"),
                        org.assertj.core.groups.Tuple.tuple(BattleOptionLabel.B, "반대")
                );

        verify(battleService).getNewBattles(List.of(
                editorPick.battleId(),
                trendingBattle.battleId(),
                bestBattle.battleId()
        ));
    }

    @Test
    @DisplayName("데이터가 없으면 빈 리스트를 반환한다")
    void getHome_returns_empty_lists_when_no_data() {
        Long userId = 1L;

        when(notificationService.hasNewBroadcast(userId, NotificationCategory.NOTICE)).thenReturn(false);
        when(battleService.getEditorPicks()).thenReturn(List.of());
        when(battleService.getTrendingBattles()).thenReturn(List.of());
        when(battleService.getBestBattles()).thenReturn(List.of());
        when(quizService.getTodayPicks(1)).thenReturn(List.of());
        when(pollService.getTodayPicks(1)).thenReturn(List.of());
        when(battleService.getNewBattles(List.of())).thenReturn(List.of());

        var response = homeService.getHome(userId);

        assertThat(response.newNotice()).isFalse();
        assertThat(response.editorPicks()).isEmpty();
        assertThat(response.trendingBattles()).isEmpty();
        assertThat(response.bestBattles()).isEmpty();
        assertThat(response.todayQuizzes()).isEmpty();
        assertThat(response.todayVotes()).isEmpty();
        assertThat(response.newBattles()).isEmpty();
    }

    private TodayBattleResponse battle(Long id, String title) {
        return new TodayBattleResponse(
                id,
                title,
                "summary",
                "thumbnail",
                10,
                20L,
                90,
                List.of(),
                List.of(
                        new TodayOptionResponse(1001L, BattleOptionLabel.A, "A", "rep-a", "stance-a", "image-a"),
                        new TodayOptionResponse(1002L, BattleOptionLabel.B, "B", "rep-b", "stance-b", "image-b")
                )
        );
    }
}
