package com.swyp.app.domain.battle.dto.response;

import com.swyp.app.domain.battle.enums.BattleType;

import java.util.List;
import java.util.UUID;

/**
 * 유저 - 배틀 요약 정보 응답
 * 역할: 홈 화면의 각 섹션 카드나 리스트에서 '미리보기' 형태로 보여줄 데이터입니다.
 */

public record BattleSummaryResponse(
        UUID battleId,              // 배틀 고유 ID
        String title,               // 배틀 제목
        String summary,             // 배틀 요약 (누군가는 이것을...)
        String thumbnailUrl,        // 카드 배경 이미지 URL
        BattleType type,            // 배틀 타입 태그 (#BATTLE, #VOTE 등)
        Integer viewCount,          // 조회수
        Long participantsCount,     // 누적 참여자 수
        Integer audioDuration,      // 오디오 소요 시간
        List<BattleTagResponse> tags, // 카테고리/인물 태그 리스트
        List<BattleOptionResponse> options // 선택지 요약 (A vs B)
) {}