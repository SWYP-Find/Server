package com.swyp.picke.domain.home.service;

import com.swyp.picke.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.picke.domain.battle.dto.response.TodayOptionResponse;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.home.dto.response.HomeBestBattleResponse;
import com.swyp.picke.domain.home.dto.response.HomeEditorPickResponse;
import com.swyp.picke.domain.home.dto.response.HomeNewBattleResponse;
import com.swyp.picke.domain.home.dto.response.HomeResponse;
import com.swyp.picke.domain.home.dto.response.HomeTodayQuizResponse;
import com.swyp.picke.domain.home.dto.response.HomeTodayVoteOptionResponse;
import com.swyp.picke.domain.home.dto.response.HomeTodayVoteResponse;
import com.swyp.picke.domain.home.dto.response.HomeTrendingResponse;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.service.NotificationService;
import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.entity.PollOption;
import com.swyp.picke.domain.poll.service.PollService;
import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizOption;
import com.swyp.picke.domain.quiz.enums.QuizOptionLabel;
import com.swyp.picke.domain.quiz.service.QuizService;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private static final int HOME_TODAY_PICK_LIMIT = 1;
    private static final String QUIZ_SUMMARY = "왼쪽과 오른쪽 중 정답을 선택하세요";
    private static final String POLL_SUMMARY = "빈칸에 들어갈 가장 적절한 답을 골라주세요";

    private final BattleService battleService;
    private final QuizService quizService;
    private final PollService pollService;
    private final NotificationService notificationService;
    private final S3PresignedUrlService s3PresignedUrlService;

    public HomeResponse getHome(Long userId) {
        boolean newNotice = false;
        if (userId != null) {
            newNotice = notificationService.hasNewBroadcast(userId, NotificationCategory.NOTICE);
        }

        List<TodayBattleResponse> editorPickRaw = battleService.getEditorPicks();
        List<TodayBattleResponse> trendingRaw = battleService.getTrendingBattles();
        List<TodayBattleResponse> bestRaw = battleService.getBestBattles();
        List<Quiz> quizRaw = quizService.getTodayPicks(HOME_TODAY_PICK_LIMIT);
        List<Poll> pollRaw = pollService.getTodayPicks(HOME_TODAY_PICK_LIMIT);

        List<Long> excludeIds = collectBattleIds(editorPickRaw, trendingRaw, bestRaw);
        List<TodayBattleResponse> newRaw = battleService.getNewBattles(excludeIds);

        return new HomeResponse(
                newNotice,
                editorPickRaw.stream().map(this::toEditorPick).toList(),
                trendingRaw.stream().map(this::toTrending).toList(),
                bestRaw.stream().map(this::toBestBattle).toList(),
                quizRaw.stream().map(this::toTodayQuiz).toList(),
                pollRaw.stream().map(this::toTodayVote).toList(),
                newRaw.stream().map(this::toNewBattle).toList()
        );
    }

    private HomeEditorPickResponse toEditorPick(TodayBattleResponse battle) {
        return new HomeEditorPickResponse(
                battle.battleId(),
                battle.thumbnailUrl(),
                findOptionTitle(battle.options(), BattleOptionLabel.A),
                findOptionTitle(battle.options(), BattleOptionLabel.B),
                battle.title(),
                battle.summary(),
                battle.tags(),
                battle.viewCount()
        );
    }

    private HomeTrendingResponse toTrending(TodayBattleResponse battle) {
        return new HomeTrendingResponse(
                battle.battleId(),
                battle.thumbnailUrl(),
                battle.title(),
                battle.tags(),
                battle.audioDuration(),
                battle.viewCount()
        );
    }

    private HomeBestBattleResponse toBestBattle(TodayBattleResponse battle) {
        return new HomeBestBattleResponse(
                battle.battleId(),
                findOptionRepresentative(battle.options(), BattleOptionLabel.A),
                findOptionRepresentative(battle.options(), BattleOptionLabel.B),
                battle.title(),
                battle.tags(),
                battle.audioDuration(),
                battle.viewCount()
        );
    }

    private HomeTodayQuizResponse toTodayQuiz(Quiz quiz) {
        List<QuizOption> options = quizService.getOptions(quiz);
        long participantsCount = quizService.countVotes(quiz);

        QuizOption optionA = findQuizOption(options, QuizOptionLabel.A);
        QuizOption optionB = findQuizOption(options, QuizOptionLabel.B);

        return new HomeTodayQuizResponse(
                quiz.getId(),
                quiz.getTitle(),
                QUIZ_SUMMARY,
                participantsCount,
                optionA != null ? optionA.getText() : null,
                optionA != null ? optionA.getDetailText() : null,
                false,
                optionB != null ? optionB.getText() : null,
                optionB != null ? optionB.getDetailText() : null,
                false
        );
    }

    private HomeTodayVoteResponse toTodayVote(Poll poll) {
        List<PollOption> options = pollService.getOptions(poll);
        long participantsCount = pollService.countVotes(poll);

        List<HomeTodayVoteOptionResponse> homeOptions = options.stream()
                .sorted(Comparator
                        .comparing((PollOption option) -> option.getDisplayOrder() == null ? Integer.MAX_VALUE : option.getDisplayOrder())
                        .thenComparing(option -> option.getLabel() == null ? "" : option.getLabel().name())
                        .thenComparing(option -> option.getId() == null ? Long.MAX_VALUE : option.getId()))
                .map(option -> new HomeTodayVoteOptionResponse(
                        BattleOptionLabel.valueOf(option.getLabel().name()),
                        option.getTitle()
                ))
                .toList();

        return new HomeTodayVoteResponse(
                poll.getId(),
                poll.getTitlePrefix(),
                poll.getTitleSuffix(),
                POLL_SUMMARY,
                participantsCount,
                homeOptions
        );
    }

    private HomeNewBattleResponse toNewBattle(TodayBattleResponse battle) {
        return new HomeNewBattleResponse(
                battle.battleId(),
                battle.thumbnailUrl(),
                battle.title(),
                battle.summary(),
                findOptionRepresentative(battle.options(), BattleOptionLabel.A),
                findOptionTitle(battle.options(), BattleOptionLabel.A),
                findRepresentativeImageUrl(battle.options(), BattleOptionLabel.A),
                findOptionRepresentative(battle.options(), BattleOptionLabel.B),
                findOptionTitle(battle.options(), BattleOptionLabel.B),
                findRepresentativeImageUrl(battle.options(), BattleOptionLabel.B),
                battle.tags(),
                battle.audioDuration(),
                battle.viewCount()
        );
    }

    private String findOptionTitle(List<TodayOptionResponse> options, BattleOptionLabel label) {
        return Optional.ofNullable(options).orElse(List.of()).stream()
                .filter(option -> option.label() == label)
                .map(TodayOptionResponse::title)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String findOptionRepresentative(List<TodayOptionResponse> options, BattleOptionLabel label) {
        return Optional.ofNullable(options).orElse(List.of()).stream()
                .filter(option -> option.label() == label)
                .map(TodayOptionResponse::representative)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String findRepresentativeImageUrl(List<TodayOptionResponse> options, BattleOptionLabel label) {
        return Optional.ofNullable(options).orElse(List.of()).stream()
                .filter(option -> option.label() == label)
                .map(TodayOptionResponse::imageUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private QuizOption findQuizOption(List<QuizOption> options, QuizOptionLabel label) {
        return options.stream()
                .filter(option -> option.getLabel() == label)
                .findFirst()
                .orElse(null);
    }

    @SafeVarargs
    private List<Long> collectBattleIds(List<TodayBattleResponse>... groups) {
        return List.of(groups).stream()
                .flatMap(List::stream)
                .map(TodayBattleResponse::battleId)
                .distinct()
                .toList();
    }
}
