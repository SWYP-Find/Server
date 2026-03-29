package com.swyp.picke.domain.battle.dto.response;

import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
/**
 * 유저 - 옵션별 실시간 통계
 * 역할: 각 선택지별로 몇 명이 선택했는지, 퍼센트(%)는 얼마인지 담습니다.
 */

public record OptionStatResponse(
        Long optionId,          // 옵션 고유 ID
        BattleOptionLabel label,// 라벨 (A, B)
        String title,           // 옵션 명칭
        Long voteCount,         // 해당 옵션의 득표 수
        Double ratio            // 해당 옵션의 득표 비율 (0~100.0)
) {}