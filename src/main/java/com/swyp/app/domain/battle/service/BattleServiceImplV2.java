package com.swyp.app.domain.battle.service;

import com.swyp.app.domain.battle.converter.BattleConverter;
import com.swyp.app.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleTag;
import com.swyp.app.domain.battle.enums.BattleStatus;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
import com.swyp.app.domain.battle.repository.BattleRepository;
import com.swyp.app.domain.battle.repository.BattleTagRepository;
import com.swyp.app.domain.tag.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleServiceImplV2 implements HomeBattleService {

    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;
    private final BattleTagRepository battleTagRepository;
    private final BattleConverter battleConverter;

    @Override
    public List<TodayBattleResponse> getEditorPicks() {
        return convertToTodayResponses(
                battleRepository.findEditorPicks(BattleStatus.PUBLISHED, PageRequest.of(0, 10))
        );
    }

    @Override
    public List<TodayBattleResponse> getTrendingBattles() {
        return convertToTodayResponses(
                battleRepository.findTrendingBattles(LocalDateTime.now().minusDays(1), PageRequest.of(0, 5))
        );
    }

    @Override
    public List<TodayBattleResponse> getBestBattles() {
        return convertToTodayResponses(
                battleRepository.findBestBattles(PageRequest.of(0, 5))
        );
    }

    @Override
    public List<TodayBattleResponse> getTodayPicks(BattleType type) {
        return convertToTodayResponses(
                battleRepository.findTodayPicks(type, LocalDate.now())
        );
    }

    @Override
    public List<TodayBattleResponse> getNewBattles(List<UUID> excludeIds) {
        List<UUID> finalExcludeIds = (excludeIds == null || excludeIds.isEmpty())
                ? List.of(UUID.randomUUID())
                : excludeIds;

        return convertToTodayResponses(
                battleRepository.findNewBattlesExcluding(finalExcludeIds, PageRequest.of(0, 10))
        );
    }

    private List<TodayBattleResponse> convertToTodayResponses(List<Battle> battles) {
        if (battles.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<Tag>> tagsByBattleId = loadTagsByBattleId(battles);
        Map<UUID, List<BattleOption>> optionsByBattleId = loadOptionsByBattleId(battles);

        return battles.stream()
                .map(battle -> battleConverter.toTodayResponse(
                        battle,
                        tagsByBattleId.getOrDefault(battle.getId(), List.of()),
                        optionsByBattleId.getOrDefault(battle.getId(), List.of())
                ))
                .toList();
    }

    private Map<UUID, List<Tag>> loadTagsByBattleId(List<Battle> battles) {
        Map<UUID, List<Tag>> tagsByBattleId = new HashMap<>();

        for (BattleTag battleTag : battleTagRepository.findByBattleIn(battles)) {
            Tag tag = battleTag.getTag();
            if (tag.getDeletedAt() != null) {
                continue;
            }

            UUID battleId = battleTag.getBattle().getId();
            tagsByBattleId.computeIfAbsent(battleId, ignored -> new ArrayList<>()).add(tag);
        }

        return tagsByBattleId;
    }

    private Map<UUID, List<BattleOption>> loadOptionsByBattleId(List<Battle> battles) {
        Map<UUID, List<BattleOption>> optionsByBattleId = new HashMap<>();

        for (BattleOption option : battleOptionRepository.findByBattleInOrderByBattleIdAscLabelAsc(battles)) {
            UUID battleId = option.getBattle().getId();
            optionsByBattleId.computeIfAbsent(battleId, ignored -> new ArrayList<>()).add(option);
        }

        optionsByBattleId.values()
                .forEach(options -> options.sort(Comparator.comparing(BattleOption::getLabel)));

        return optionsByBattleId;
    }
}
