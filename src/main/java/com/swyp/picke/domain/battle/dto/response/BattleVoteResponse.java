package com.swyp.picke.domain.battle.dto.response;

import java.util.List;

/**
 * 유저 - 투표 결과 전체 응답
 * 역할: 투표 완료 후 실시간으로 변한 전체 참여자 수와 옵션별 비율을 반환합니다.
 */

public record BattleVoteResponse(
        Long battleId,              // 투표한 배틀 ID
        Long selectedOptionId,      // 유저가 방금 선택한 옵션 ID
        Long totalParticipants,     // 실시간 전체 참여자 수
        List<OptionStatResponse> results // 옵션별 득표 현황 리스트
) {}