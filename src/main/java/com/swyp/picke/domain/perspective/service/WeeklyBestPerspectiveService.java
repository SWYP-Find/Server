package com.swyp.picke.domain.perspective.service;

import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.perspective.dto.response.WeeklyBestPerspectiveResponse;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.PerspectiveLikeRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.perspective.repository.projection.WeeklyPerspectiveLikeCount;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.repository.UserBattleRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직전 완료된 한 주(월~일, KST) 동안 좋아요를 많이 받은 일반 배틀 관점을 조회한다.
 * 집계 테이블 없이 기간을 고정해 계산하므로 숨김 처리된 관점은 바로 빠진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyBestPerspectiveService {

    private static final int MAX_ITEMS = 25;

    private final PerspectiveLikeRepository perspectiveLikeRepository;
    private final PerspectiveRepository perspectiveRepository;
    private final UserBattleRepository userBattleRepository;
    private final Clock clock;

    public WeeklyBestPerspectiveResponse getWeeklyBest(Long userId) {
        LocalDate thisMonday = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekStart = thisMonday.minusWeeks(1);

        List<WeeklyPerspectiveLikeCount> ranking = perspectiveLikeRepository.findWeeklyLikeRanking(
                weekStart.atStartOfDay(),
                thisMonday.atStartOfDay(),
                PerspectiveStatus.PUBLISHED,
                BattleStatus.PUBLISHED,
                PageRequest.of(0, MAX_ITEMS));
        if (ranking.isEmpty()) {
            return new WeeklyBestPerspectiveResponse(weekStart, thisMonday.minusDays(1), List.of());
        }

        Map<Long, Perspective> perspectives = perspectiveRepository
                .findAllWithBattleByIdIn(ranking.stream().map(WeeklyPerspectiveLikeCount::getPerspectiveId).toList())
                .stream()
                .collect(Collectors.toMap(Perspective::getId, Function.identity()));
        Set<Long> participatedBattleIds = findParticipatedBattleIds(userId, perspectives.values());

        List<WeeklyBestPerspectiveResponse.Item> items = ranking.stream()
                .map(rank -> {
                    Perspective perspective = perspectives.get(rank.getPerspectiveId());
                    Long battleId = perspective.getBattle().getId();
                    return new WeeklyBestPerspectiveResponse.Item(
                            perspective.getId(),
                            battleId,
                            perspective.getBattle().getTitle(),
                            perspective.getContent(),
                            rank.getLikeCount(),
                            participatedBattleIds.contains(battleId));
                })
                .toList();
        return new WeeklyBestPerspectiveResponse(weekStart, thisMonday.minusDays(1), items);
    }

    // 참여 기준은 관점 작성 조건과 같은 사후투표 완료(COMPLETED)다. 비로그인은 미참여로 본다.
    private Set<Long> findParticipatedBattleIds(Long userId, java.util.Collection<Perspective> perspectives) {
        if (userId == null) {
            return Set.of();
        }
        Set<Long> battleIds = perspectives.stream()
                .map(perspective -> perspective.getBattle().getId())
                .collect(Collectors.toSet());
        return userBattleRepository.findBattleIdsByUserAndStep(userId, battleIds, UserBattleStep.COMPLETED);
    }
}
