package com.swyp.picke.domain.battle.dto.response;

import java.util.List;

/**
 * 유저 - 오늘의 배틀 목록 응답
 * 역할: 오늘의 배틀 섹션에 노출될 배틀들과 총 개수를 감싸는 리스트형 DTO입니다.
 */

public record TodayBattleListResponse(
        List<TodayBattleResponse> items, // 오늘의 배틀 리스트
        Integer totalCount               // 목록 총 개수
) {}