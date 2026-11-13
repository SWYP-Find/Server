package com.swyp.picke.domain.classroom.enums;

public enum AiJobType {
    TOPIC_CANDIDATES,
    TOPIC_VALIDATION,
    DEBATE_ARGUMENT,   // AI 철학자 반박 (200~500자)
    DEBATE_INPUT_CLASSIFY, // 참가자 반박 입력 분류 (엣지 케이스 PRD 4.1). 반려된 반박도 기록한다
    MEMBER_REPORT,
    CLASS_INSIGHT
}
