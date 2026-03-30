package com.swyp.picke.domain.battle.dto.response;

import com.swyp.picke.domain.battle.enums.BattleType;

import java.util.List;

/**
 * 유저 - 오늘의 배틀 상세 응답 (시안 6번)
 * 역할: 어두운 배경의 풀스크린 UI에 필요한 배경 이미지, 시간 등을 담습니다.
 */
public record TodayBattleResponse(
        Long battleId,          // 배틀 고유 ID
        String title,           // 배틀 제목
        String summary,         // 중간 요약 문구
        String thumbnailUrl,    // 풀스크린 배경 이미지 URL
        BattleType type,        // 타입 태그
        Integer viewCount,      // 조회수
        Long participantsCount, // 누적 참여자 수
        Integer audioDuration,  // 소요 시간 (분:초 변환용 데이터)
        List<BattleTagResponse> tags,       // 상단 태그 리스트
        List<TodayOptionResponse> options,  // 중앙 세로형 대결 카드 데이터
        // 퀴즈·투표 전용 필드
        String titlePrefix,     // 투표 접두사 (예: "도덕의 기준은")
        String titleSuffix,     // 투표 접미사 (예: "이다")
        String itemA,           // 퀴즈 O 선택지
        String itemADesc,       // 퀴즈 O 설명
        String itemB,           // 퀴즈 X 선택지
        String itemBDesc        // 퀴즈 X 설명
) {}
