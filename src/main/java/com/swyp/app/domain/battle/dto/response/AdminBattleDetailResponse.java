package com.swyp.app.domain.battle.dto.response;

import com.swyp.app.domain.battle.enums.BattleCreatorType;
import com.swyp.app.domain.battle.enums.BattleStatus;
import com.swyp.app.domain.battle.enums.BattleType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 - 배틀 상세 상세 조회 응답
 * 역할: 관리자가 배틀의 모든 설정 값(상태, 생성자 타입, 수정일 등)을 확인하고 수정할 때 사용합니다.
 */

public record AdminBattleDetailResponse(
        Long battleId,                  // 배틀 고유 ID
        String title,                   // 배틀 제목
        String summary,                 // 배틀 요약 문구
        String description,             // 배틀 상세 설명
        String thumbnailUrl,            // 상단 배경 이미지 URL
        BattleType type,                // 배틀 타입 (BATTLE, QUIZ, VOTE)
        LocalDate targetDate,           // 게시 예정일 (홈 화면 노출 날짜)
        BattleStatus status,            // 배틀 상태 (DRAFT, PUBLISHED, ARCHIVED 등)
        BattleCreatorType creatorType,  // 생성 주체 (ADMIN, USER)
        List<BattleTagResponse> tags,   // 연결된 모든 태그 리스트
        List<BattleOptionResponse> options, // 대결 선택지 상세 정보 리스트
        LocalDateTime createdAt,        // 데이터 생성 일시
        LocalDateTime updatedAt         // 데이터 최종 수정 일시
) {}