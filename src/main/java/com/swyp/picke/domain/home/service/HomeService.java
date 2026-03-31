package com.swyp.picke.domain.home.service;

import com.swyp.picke.domain.battle.dto.response.BattleTagResponse;
import com.swyp.picke.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.picke.domain.battle.dto.response.TodayOptionResponse;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleType;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.home.dto.response.*;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.service.NotificationService;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final BattleService battleService;
    private final NotificationService notificationService;
    private final S3PresignedUrlService s3PresignedUrlService;

    public HomeResponse getHome() {
        boolean newNotice = notificationService.hasNewBroadcast(NotificationCategory.NOTICE);

        // DB 쿼리 단계에서 LIMIT을 걸어 필요한 개수만 깔끔하게 조회!
        List<TodayBattleResponse> editorPickRaw = battleService.getEditorPicks(10);
        List<TodayBattleResponse> trendingRaw = battleService.getTrendingBattles(4);
        List<TodayBattleResponse> bestRaw = battleService.getBestBattles(3);
        List<TodayBattleResponse> voteRaw = battleService.getTodayPicks(BattleType.VOTE, 1);
        List<TodayBattleResponse> quizRaw = battleService.getTodayPicks(BattleType.QUIZ, 1);

        List<Long> excludeIds = collectBattleIds(editorPickRaw, trendingRaw, bestRaw, voteRaw, quizRaw);
        List<TodayBattleResponse> newRaw = battleService.getNewBattles(excludeIds, 3);

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

    // 에디터픽 썸네일 Presigned URL 적용
    private HomeEditorPickResponse toEditorPick(TodayBattleResponse b) {
        String optionA = findOptionTitle(b.options(), BattleOptionLabel.A);
        String optionB = findOptionTitle(b.options(), BattleOptionLabel.B);

        String secureThumb = b.thumbnailUrl();

        return new HomeEditorPickResponse(
                b.battleId(), secureThumb,
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
        String philoA = findOptionRepresentative(b.options(), BattleOptionLabel.A);
        String philoB = findOptionRepresentative(b.options(), BattleOptionLabel.B);

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
        List<HomeTodayVoteOptionResponse> options = Optional.ofNullable(b.options()).orElse(List.of()).stream()
                .map(o -> new HomeTodayVoteOptionResponse(o.label(), o.title()))
                .toList();
        return new HomeTodayVoteResponse(
                b.battleId(),
                b.titlePrefix(), b.titleSuffix(),
                b.summary(), b.participantsCount(),
                options
        );
    }

    // newBattle 썸네일 Presigned URL 적용
    private HomeNewBattleResponse toNewBattle(TodayBattleResponse b) {
        String philoA = findOptionRepresentative(b.options(), BattleOptionLabel.A);
        String philoB = findOptionRepresentative(b.options(), BattleOptionLabel.B);

        String optionA = findOptionTitle(b.options(), BattleOptionLabel.A);
        String optionB = findOptionTitle(b.options(), BattleOptionLabel.B);

        String imageA = findRepresentativeImageUrl(b.options(), BattleOptionLabel.A);
        String imageB = findRepresentativeImageUrl(b.options(), BattleOptionLabel.B);

        return new HomeNewBattleResponse(
                b.battleId(), b.thumbnailUrl(),
                b.title(), b.summary(),
                philoA, optionA, imageA,
                philoB, optionB, imageB,
                b.tags(), b.audioDuration(), b.viewCount()
        );
    }

    private String findOptionTitle(List<TodayOptionResponse> options, BattleOptionLabel label) {
        return Optional.ofNullable(options).orElse(List.of()).stream()
                .filter(o -> o.label() == label)
                .map(TodayOptionResponse::title)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    // 옵션에서 철학자 이름(Representative)을 추출하는 메서드
    private String findOptionRepresentative(List<TodayOptionResponse> options, BattleOptionLabel label) {
        return Optional.ofNullable(options).orElse(List.of()).stream()
                .filter(o -> o.label() == label)
                .map(TodayOptionResponse::representative)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private List<String> findPhilosopherNames(List<BattleTagResponse> tags) {
        return Optional.ofNullable(tags).orElse(List.of()).stream()
                .filter(t -> t.type() == TagType.PHILOSOPHER)
                .map(BattleTagResponse::name)
                .toList();
    }

    private String findRepresentativeImageUrl(List<TodayOptionResponse> options, BattleOptionLabel label) {
        return Optional.ofNullable(options).orElse(List.of()).stream()
                .filter(o -> o.label() == label)
                .map(TodayOptionResponse::imageUrl)
                .filter(Objects::nonNull)
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
