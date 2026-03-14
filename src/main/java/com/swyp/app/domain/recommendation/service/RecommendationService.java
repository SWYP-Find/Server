package com.swyp.app.domain.recommendation.service;

import com.swyp.app.domain.recommendation.dto.response.RecommendationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    public RecommendationListResponse getSimilarBattles(UUID battleId) {
        // TODO: Battle 엔티티 병합 후 배틀 존재 여부 검증 (ErrorCode.BATTLE_NOT_FOUND)
        // TODO: 현재 로그인 유저의 성향 점수 조회 (User 병합 후)
        // TODO: 유사 성향 유저들이 참여한 배틀 목록 조회 로직 구현 (Battle, Vote 병합 후)
        // TODO: 각 배틀의 tags, options, participantsCount 조회 (Battle, BattleOption, Tag 병합 후)

        return new RecommendationListResponse(List.of());
    }
}
