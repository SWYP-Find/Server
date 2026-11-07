package com.swyp.picke.domain.classroom.enums;

/** 주제 초안의 마지막 AI 주제 제안 결과. 실패해도 이전 성공 배치의 후보는 그대로 둔다 (PRD §10) */
public enum ClassTopicSuggestStatus {
    NONE,                 // 아직 제안을 요청하지 않음
    GENERATING,
    SUCCEEDED,
    NEEDS_CLARIFICATION,  // 맥락이 부족해 후보 없이 보완 안내만 받음
    FAILED                // 제공자 오류 또는 검증 실패. 이전 후보 유지, 횟수 차감 없음
}
