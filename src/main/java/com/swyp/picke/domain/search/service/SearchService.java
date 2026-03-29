package com.swyp.picke.domain.search.service;

import com.swyp.picke.domain.battle.dto.response.BattleTagResponse;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleTag;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.battle.repository.BattleTagRepository;
import com.swyp.picke.domain.search.dto.response.SearchBattleListResponse;
import com.swyp.picke.domain.search.enums.SearchSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final BattleRepository battleRepository;
    private final BattleTagRepository battleTagRepository;

    public SearchBattleListResponse searchBattles(String category, SearchSortType sort, Integer offset, Integer size) {
        int pageOffset = offset == null || offset < 0 ? 0 : offset;
        int pageSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
        SearchSortType sortType = sort == null ? SearchSortType.POPULAR : sort;

        Sort pageSort = sortType == SearchSortType.LATEST
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "viewCount");
        Pageable pageable = PageRequest.of(pageOffset / pageSize, pageSize, pageSort);

        List<Battle> battles;
        long totalCount;

        if (category == null || category.isBlank()) {
            battles = battleRepository.searchAll(pageable);
            totalCount = battleRepository.countSearchAll();
        } else {
            battles = battleRepository.searchByCategory(category, pageable);
            totalCount = battleRepository.countSearchByCategory(category);
        }

        Map<Long, List<BattleTagResponse>> tagMap = loadTagMap(battles);

        List<SearchBattleListResponse.SearchBattleItem> items = battles.stream()
                .map(battle -> new SearchBattleListResponse.SearchBattleItem(
                        battle.getId(),
                        battle.getThumbnailUrl(),
                        battle.getType(),
                        battle.getTitle(),
                        battle.getSummary(),
                        tagMap.getOrDefault(battle.getId(), List.of()),
                        battle.getAudioDuration(),
                        battle.getViewCount()
                ))
                .toList();

        int nextOffset = pageOffset + pageSize;
        boolean hasNext = nextOffset < totalCount;
        return new SearchBattleListResponse(items, hasNext ? nextOffset : null, hasNext);
    }

    private Map<Long, List<BattleTagResponse>> loadTagMap(List<Battle> battles) {
        List<Long> battleIds = battles.stream().map(Battle::getId).toList();
        if (battleIds.isEmpty()) return Map.of();

        List<BattleTag> battleTags = battleTagRepository.findByBattleIdIn(battleIds);
        return battleTags.stream()
                .collect(Collectors.groupingBy(
                        bt -> bt.getBattle().getId(),
                        Collectors.mapping(
                                bt -> new BattleTagResponse(
                                        bt.getTag().getId(),
                                        bt.getTag().getName(),
                                        bt.getTag().getType()
                                ),
                                Collectors.toList()
                        )
                ));
    }
}
