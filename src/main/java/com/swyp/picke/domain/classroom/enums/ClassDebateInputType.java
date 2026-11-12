package com.swyp.picke.domain.classroom.enums;

/**
 * 참가자 반박의 입력 유형 (엣지 케이스 PRD 2.1 EC-01~08, 4.1 input_type). 한 반박에 여러 유형이 함께 붙을 수 있다.
 * NORMAL ~ MEANINGLESS 는 내용 유형 하나, PROFANITY·AGGRESSIVE·INJECTION 은 해당하면 더한다.
 */
public enum ClassDebateInputType {
    NORMAL,        // EC-01 주장과 이유가 있음
    SHORT_VALID,   // EC-02 짧지만 입장이나 이유를 알 수 있음
    INSUFFICIENT,  // EC-03 입장만 있거나 이유가 불분명
    OFF_TOPIC,     // EC-04 주제와 무관
    PROFANITY,     // EC-05 비속어 포함
    AGGRESSIVE,    // EC-06 공격·혐오 표현 (차단 대상이 아닌 것)
    MEANINGLESS,   // EC-07 무의미한 입력 (반려)
    INJECTION;     // EC-08 프롬프트 인젝션 (반려)

    /** 리포트 분석에 쓸 수 있는 내용 유형 (4.1 is_valid_for_analysis) */
    public boolean isAnalyzable() {
        return this == NORMAL || this == SHORT_VALID;
    }
}
