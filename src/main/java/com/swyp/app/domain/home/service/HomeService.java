package com.swyp.app.domain.home.service;

import com.swyp.app.domain.battle.dto.response.BattleTagResponse;
import com.swyp.app.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.app.domain.battle.dto.response.TodayOptionResponse;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.tag.enums.TagType;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.home.dto.response.*;
import com.swyp.app.domain.notification.enums.NotificationCategory;
import com.swyp.app.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final BattleService battleService;
    private final NotificationService notificationService;

    public HomeResponse getHome() {
        boolean newNotice = notificationService.hasNewBroadcast(NotificationCategory.NOTICE);

        List<TodayBattleResponse> editorPickRaw = battleService.getEditorPicks();
        List<TodayBattleResponse> trendingRaw = battleService.getTrendingBattles();
        List<TodayBattleResponse> bestRaw = battleService.getBestBattles();
        List<TodayBattleResponse> voteRaw = battleService.getTodayPicks(BattleType.VOTE);
        List<TodayBattleResponse> quizRaw = battleService.getTodayPicks(BattleType.QUIZ);

        List<Long> excludeIds = collectBattleIds(editorPickRaw, trendingRaw, bestRaw, voteRaw, quizRaw);
        List<TodayBattleResponse> newRaw = battleService.getNewBattles(excludeIds);

        return new HomeResponse(
                newNotice,
                editorPickRaw.stream().map(this::toEditorPick).toList(),
                trendingRaw.stream().map(this::toTrending).toList(),
                bestRaw.stream().map(this::toBestBattle).toList(),
                quizRaw.stream().map(this::toTodayQuiz).toList(),
                voteRaw.stream().map(this::toTodayVote).toList(),
                newRaw.stream().map(this::toNewBattle).toList()
        );
    }

    private HomeEditorPickResponse toEditorPick(TodayBattleResponse b) {
        String optionA = findOptionTitle(b.options(), BattleOptionLabel.A);
        String optionB = findOptionTitle(b.options(), BattleOptionLabel.B);
        return new HomeEditorPickResponse(
                b.battleId(), b.thumbnailUrl(),
                optionA, optionB,
                b.title(), b.summary(),
                b.tags(), b.viewCount()
        );
    }

    private HomeTrendingResponse toTrending(TodayBattleResponse b) {
        return new HomeTrendingResponse(
                b.battleId(), b.thumbnailUrl(),
                b.title(), b.tags(),
                b.audioDuration(), b.viewCount()
        );
    }

    private HomeBestBattleResponse toBestBattle(TodayBattleResponse b) {
        List<String> philosophers = findPhilosopherNames(b.tags());
        String philoA = philosophers.size() > 0 ? philosophers.get(0) : null;
        String philoB = philosophers.size() > 1 ? philosophers.get(1) : null;
        return new HomeBestBattleResponse(
                b.battleId(),
                philoA, philoB,
                b.title(), b.tags(),
                b.audioDuration(), b.viewCount()
        );
    }

    private HomeTodayQuizResponse toTodayQuiz(TodayBattleResponse b) {
        return new HomeTodayQuizResponse(
                b.battleId(), b.title(), b.summary(),
                b.participantsCount(),
                b.itemA(), b.itemADesc(),
                b.itemB(), b.itemBDesc()
        );
    }

    private HomeTodayVoteResponse toTodayVote(TodayBattleResponse b) {
        List<HomeTodayVoteOptionResponse> options = b.options().stream()
                .map(o -> new HomeTodayVoteOptionResponse(o.label(), o.title()))
                .toList();
        return new HomeTodayVoteResponse(
                b.battleId(),
                b.titlePrefix(), b.titleSuffix(),
                b.summary(), b.participantsCount(),
                options
        );
    }

    private HomeNewBattleResponse toNewBattle(TodayBattleResponse b) {
        List<String> philosophers = findPhilosopherNames(b.tags());
        String philoA = philosophers.size() > 0 ? philosophers.get(0) : null;
        String philoB = philosophers.size() > 1 ? philosophers.get(1) : null;
        String imageA = findOptionImageUrl(b.options(), BattleOptionLabel.A);
        String imageB = findOptionImageUrl(b.options(), BattleOptionLabel.B);
        return new HomeNewBattleResponse(
                b.battleId(), b.thumbnailUrl(),
                b.title(), b.summary(),
                philoA, imageA,
                philoB, imageB,
                b.tags(), b.audioDuration(), b.viewCount()
        );
    }

    private String findOptionTitle(List<TodayOptionResponse> options, BattleOptionLabel label) {
        return Optional.ofNullable(options).orElse(List.of()).stream()
                .filter(o -> o.label() == label)
                .map(TodayOptionResponse::title)
                .findFirst().orElse(null);
    }

    private List<String> findPhilosopherNames(List<BattleTagResponse> tags) {
        return Optional.ofNullable(tags).orElse(List.of()).stream()
                .filter(t -> t.type() == TagType.PHILOSOPHER)
                .map(BattleTagResponse::name)
                .toList();
    }

    private String findOptionImageUrl(List<TodayOptionResponse> options, BattleOptionLabel label) {
        return Optional.ofNullable(options).orElse(List.of()).stream()
                .filter(o -> o.label() == label)
                .map(TodayOptionResponse::imageUrl)
                .findFirst().orElse(null);
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
