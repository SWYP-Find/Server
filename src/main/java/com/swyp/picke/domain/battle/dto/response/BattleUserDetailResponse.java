package com.swyp.picke.domain.battle.dto.response;

import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.VoteSide;

import java.util.List;

/**
 * 유저 - 배틀 상세 페이지 응답 (시안 4, 5번)
 * 역할: 배틀 클릭 시 진입하는 상세 화면의 모든 정보를 담습니다. 투표 여부에 따라 UI가 변합니다.
 */
public record BattleUserDetailResponse(
        BattleSummaryResponse battleInfo, // 기본적인 배틀 정보 (요약 DTO 재사용)
        String titlePrefix,
        String titleSuffix,
        String itemA,
        String itemADesc,
        String itemB,
        String itemBDesc,
        String description,               // 상세 본문 설명
        String shareUrl,                  // 공유하기 버튼용 링크
        VoteSide userVoteStatus,            // 현재 유저의 투표 상태
        UserBattleStep currentStep,
        List<BattleTagResponse> categoryTags,    // UI 상단용 카테고리 태그
        List<BattleTagResponse> philosopherTags, // UI 하단용 철학자 태그
        List<BattleTagResponse> valueTags        // 성향 분석용 가치관 태그
) {}