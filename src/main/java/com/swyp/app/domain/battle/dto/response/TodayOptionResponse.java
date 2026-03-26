package com.swyp.app.domain.battle.dto.response;

import com.swyp.app.domain.battle.enums.BattleOptionLabel;

/**
 * 유저 - 오늘의 배틀 전용 옵션 응답
 * 역할: 오늘의 배틀 시안의 세로형 카드에 들어가는 인물, 입장, 아바타 정보를 담습니다.
 */

public record TodayOptionResponse(
        Long optionId,          // 옵션 ID
        BattleOptionLabel label,// 라벨 (A, B)
        String title,           // 제목 (예: 찬성한다)
        String representative,  // 인물 (예: 피터 싱어)
        String stance,          // 한 줄 입장 (예: 고통을 끝낼 권리는..)
        String imageUrl         // 아바타 이미지 URL
) {}
