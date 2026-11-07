package com.swyp.picke.domain.classroom.enums;

/** 주제 제안이 FAILED 로 끝난 이유. 화면 안내를 나누기 위한 값이며 제공자 오류 상세는 ai_jobs.error_code 에 남긴다 (#608) */
public enum ClassTopicSuggestFailureReason {
    AI_UNAVAILABLE,  // 제공자 장애(연결 오류·시간 초과·5xx·크레딧 소진 등). 잠시 후 다시 시도
    INVALID_OUTPUT   // AI 응답이 1회 보정 후에도 후보 규칙을 어김. 다시 시도하거나 조건을 바꿔 요청
}
