package com.swyp.app.domain.battle.dto.response;

import com.swyp.app.domain.battle.enums.BattleType;

import java.util.List;
import java.util.UUID;

/**
 * 유저 - 오늘의 배틀 상세 응답 (시안 6번)
 * 역할: 어두운 배경의 풀스크린 UI에 필요한 배경 이미지, 시간, 공유 주소 등을 담습니다.
 */

public record TodayBattleResponse(
        UUID battleId,          // 배틀 고유 ID
        String title,           // 배틀 제목
        String summary,         // 중간 요약 문구
        String thumbnailUrl,    // 풀스크린 배경 이미지 URL
        BattleType type,        // 타입 태그
        Integer audioDuration,  // 소요 시간 (분:초 변환용 데이터)
        String shareUrl,        // 공유하기 링크
        List<BattleTagResponse> tags,       // 상단 태그 리스트
        List<TodayOptionResponse> options   // 중앙 세로형 대결 카드 데이터
) {}