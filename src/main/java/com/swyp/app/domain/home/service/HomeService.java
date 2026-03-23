package com.swyp.app.domain.home.service;

import com.swyp.app.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.app.domain.battle.dto.response.TodayOptionResponse;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.home.dto.response.HomeBattleOptionResponse;
import com.swyp.app.domain.home.dto.response.HomeBattleResponse;
import com.swyp.app.domain.home.dto.response.HomeResponse;
import com.swyp.app.domain.notice.enums.NoticePlacement;
import com.swyp.app.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private static final int NOTICE_EXISTS_LIMIT = 1;

    private final BattleService battleService;
    private final NoticeService noticeService;

    public HomeResponse getHome() {
        boolean newNotice = !noticeService.getActiveNotices(NoticePlacement.HOME_TOP, null, NOTICE_EXISTS_LIMIT).isEmpty();

        List<HomeBattleResponse> editorPicks = toHomeBattles(battleService.getEditorPicks());
        List<HomeBattleResponse> trendingBattles = toHomeBattles(battleService.getTrendingBattles());
        List<HomeBattleResponse> bestBattles = toHomeBattles(battleService.getBestBattles());

        List<HomeBattleResponse> todayPicks = new ArrayList<>();
        todayPicks.addAll(toHomeBattles(battleService.getTodayPicks(BattleType.VOTE)));
        todayPicks.addAll(toHomeBattles(battleService.getTodayPicks(BattleType.QUIZ)));

        List<Long> excludeIds = collectBattleIds(editorPicks, trendingBattles, bestBattles, todayPicks);
        List<HomeBattleResponse> newBattles = toHomeBattles(battleService.getNewBattles(excludeIds));

        return new HomeResponse(
                newNotice,
                editorPicks,
                trendingBattles,
                bestBattles,
                todayPicks,
                newBattles
        );
    }

    private List<HomeBattleResponse> toHomeBattles(List<TodayBattleResponse> battles) {
        return battles.stream()
                .map(this::toHomeBattle)
                .toList();
    }

    private HomeBattleResponse toHomeBattle(TodayBattleResponse battle) {
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

    private HomeBattleOptionResponse toHomeOption(TodayOptionResponse option) {
        return new HomeBattleOptionResponse(option.label(), option.title());
    }

    @SafeVarargs
    private List<Long> collectBattleIds(List<HomeBattleResponse>... groups) {
        return List.of(groups).stream()
                .flatMap(List::stream)
                .map(HomeBattleResponse::battleId)
                .distinct()
                .toList();
    }
}
