package com.swyp.picke.domain.battle.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.entity.BattleOptionTag;
import com.swyp.picke.domain.battle.entity.BattleTag;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.battle.repository.BattleTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swyp.picke.domain.tag.enums.TagType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleQueryService {

    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;
    private final BattleTagRepository battleTagRepository;
    private final BattleOptionTagRepository battleOptionTagRepository;

    public Map<Long, Battle> findBattlesByIds(List<Long> battleIds) {
        return battleRepository.findAllById(battleIds).stream()
                .collect(Collectors.toMap(Battle::getId, Function.identity()));
    }

    public Map<Long, BattleOption> findOptionsByIds(List<Long> optionIds) {
        return battleOptionRepository.findAllById(optionIds).stream()
                .collect(Collectors.toMap(BattleOption::getId, Function.identity()));
    }

    /**
     * 주어진 배틀 ID 목록에 대해 태그별 빈도를 집계하여 상위 limit개를 반환한다.
     * @return Map<태그명, 빈도수> (상위 limit개)
     */
    public Map<String, Long> getTopTagsByBattleIds(List<Long> battleIds, int limit) {
        if (battleIds.isEmpty()) return Map.of();

        List<BattleTag> battleTags = battleTagRepository.findByBattleIdIn(battleIds);

        return battleTags.stream()
                .collect(Collectors.groupingBy(
                        bt -> bt.getTag().getName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ));
    }

    public Map<Long, String> getCategoryNamesByBattleIds(List<Long> battleIds) {
        if (battleIds == null || battleIds.isEmpty()) return Map.of();

        return battleTagRepository.findByBattleIdIn(battleIds).stream()  // findByBattleIdInWithTag → findByBattleIdIn
                .filter(bt -> bt.getTag().getType() == TagType.CATEGORY)
                .collect(Collectors.toMap(
                        bt -> bt.getBattle().getId(),
                        bt -> bt.getTag().getName(),
                        (a, b) -> a
                ));
    }

    public Optional<String> getTopPhilosopherTagNameFromOptions(List<Long> optionIds) {
        if (optionIds.isEmpty()) return Optional.empty();

        List<BattleOptionTag> optionTags = battleOptionTagRepository.findByBattleOptionIdIn(optionIds);

        return optionTags.stream()
                .filter(bot -> bot.getTag().getType() == TagType.PHILOSOPHER)
                .collect(Collectors.groupingBy(
                        bot -> bot.getTag().getName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }
}
