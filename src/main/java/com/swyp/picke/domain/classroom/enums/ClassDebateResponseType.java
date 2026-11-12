package com.swyp.picke.domain.classroom.enums;

/**
 * 반박에 이어질 AI 응답의 성격 (엣지 케이스 PRD 4.1 response_type). 입력 분류로 서버가 정한다.
 * 3번째 반박은 AI 응답이 없어 null 이다. 반려된 입력(입력 제한)은 저장하지 않는다.
 */
public enum ClassDebateResponseType {
    NORMAL,          // 정상 반론
    CLARIFY,         // 논거 구체화 질문 (EC-03)
    RETURN_TO_TOPIC  // 주제 복귀 질문 (EC-04)
}
