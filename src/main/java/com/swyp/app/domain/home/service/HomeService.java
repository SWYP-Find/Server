package com.swyp.app.domain.home.service;

import com.swyp.app.domain.battle.dto.response.BattleOptionResponse;
import com.swyp.app.domain.battle.dto.response.BattleSummaryResponse;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.home.dto.response.HomeBattleOptionResponse;
import com.swyp.app.domain.home.dto.response.HomeBattleResponse;
import com.swyp.app.domain.home.dto.response.HomeResponse;
import com.swyp.app.domain.notice.entity.NoticePlacement;
import com.swyp.app.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private static final int NOTICE_EXISTS_LIMIT = 1;

    private final BattleService battleService;
    private final NoticeService noticeService;

    public HomeResponse getHome() {
        boolean newNotice = !noticeService.getActiveNotices(NoticePlacement.HOME_TOP, null, NOTICE_EXISTS_LIMIT).isEmpty();

        List<HomeBattleResponse> editorPicks = toHomeBattles(battleService.getHomeEditorPicks());
        List<HomeBattleResponse> trendingBattles = toHomeBattles(battleService.getHomeTrendingBattles());
        List<HomeBattleResponse> bestBattles = toHomeBattles(battleService.getHomeBestBattles());

        List<HomeBattleResponse> todayPicks = new ArrayList<>();
        todayPicks.addAll(toHomeBattles(battleService.getHomeTodayPicks(BattleType.VOTE)));
        todayPicks.addAll(toHomeBattles(battleService.getHomeTodayPicks(BattleType.QUIZ)));

        List<UUID> excludeIds = collectBattleIds(editorPicks, trendingBattles, bestBattles, todayPicks);
        List<HomeBattleResponse> newBattles = toHomeBattles(battleService.getHomeNewBattles(excludeIds));

        return new HomeResponse(
                newNotice,
                editorPicks,
                trendingBattles,
                bestBattles,
                todayPicks,
                newBattles
        );
    }

    private List<HomeBattleResponse> toHomeBattles(List<BattleSummaryResponse> battles) {
        return battles.stream()
                .map(this::toHomeBattle)
                .toList();
    }

    private HomeBattleResponse toHomeBattle(BattleSummaryResponse battle) {
        return new HomeBattleResponse(
                battle.battleId(),
                battle.title(),
                battle.summary(),
                battle.thumbnailUrl(),
                battle.type(),
                battle.viewCount(),
                battle.participantsCount(),
                battle.audioDuration(),
                battle.tags(),
                battle.options().stream()
                        .map(this::toHomeOption)
                        .toList()
        );
    }

    private HomeBattleOptionResponse toHomeOption(BattleOptionResponse option) {
        return new HomeBattleOptionResponse(option.label(), option.title());
    }

    @SafeVarargs
    private List<UUID> collectBattleIds(List<HomeBattleResponse>... groups) {
        return List.of(groups).stream()
                .flatMap(List::stream)
                .map(HomeBattleResponse::battleId)
                .distinct()
                .toList();
    }
}
