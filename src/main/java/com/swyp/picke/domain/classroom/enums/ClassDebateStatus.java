package com.swyp.picke.domain.classroom.enums;

/**
 * 토론 세션 상태 (PRD §8). 라운드 번호는 세션의 round 컬럼으로 함께 본다.
 * GENERATING(n) = PRD 의 GENERATING_AI_n, WAITING_USER(n) = WAIT_USER_n.
 */
public enum ClassDebateStatus {
    GENERATING,    // n 라운드 AI 주장 생성 중
    WAITING_USER,  // n 라운드 AI 주장 표시, 참가자 반박 대기
    FAILED,        // n 라운드 AI 주장 생성 실패. 같은 작업으로 재시도 가능
    COMPLETED      // 3라운드 반박까지 끝남 (DEBATE_COMPLETED). 사후투표 가능
}
