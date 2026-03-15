package com.swyp.app.domain.battle.dto.response;

import java.util.List;

/**
 * 유저 - 배틀 상세 페이지 응답 (시안 4, 5번)
 * 역할: 배틀 클릭 시 진입하는 상세 화면의 모든 정보를 담습니다. 투표 여부에 따라 UI가 변합니다.
 */

public record BattleUserDetailResponse(
        BattleSummaryResponse battleInfo, // 기본적인 배틀 정보 (요약 DTO 재사용)
        String description,               // 상세 본문 설명
        String shareUrl,                  // 공유하기 버튼용 링크
        String userVoteStatus,            // 현재 유저의 투표 상태 (NONE, A, B...)
        List<BattleTagResponse> categoryTags,    // UI 상단용 카테고리 태그만 분리
        List<BattleTagResponse> philosopherTags, // UI 하단용 철학자 태그만 분리
        List<BattleTagResponse> valueTags        // 성향 분석용 가치관 태그만 분리
) {}