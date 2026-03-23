package com.swyp.app.domain.recommendation.service;

import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.recommendation.dto.response.RecommendationListResponse;
import com.swyp.app.domain.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final BattleService battleService;
    private final TagService tagService;

    public RecommendationListResponse getInterestingBattles(Long battleId, String cursor, Integer size) {
        battleService.findById(battleId);

        // TODO: 흥미 기반 배틀 추천 정책 미확정 (추후 구현)

        return new RecommendationListResponse(List.of(), null, false);
    }
}
