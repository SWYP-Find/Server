package com.swyp.picke.domain.classroom.enums;

/**
 * 결과 화면에 내려주는 리포트 상태. DB 상태(ClassReportStatus)에 "마감 전"과 "대상 아님"을 더한 것 (PRD S15 "생성 대기·실패·자료 부족·일부 분석 구분").
 */
public enum ClassReportViewState {
    BEFORE_CLOSE,   // 마감 전. "{마감 시각} 이후 AI 피드백이 제공돼요"
    NOT_ELIGIBLE,   // 참여 미완료라 개인 리포트를 만들지 않음 (PRD §15)
    PENDING,        // 생성 대기
    GENERATING,     // 생성 중. "생각을 정리하고 있어요"
    SUCCEEDED,
    LIMITED,        // 자료 부족 템플릿. "판단의 이유를 살펴볼 글이 아직 충분하지 않아요"
    FAILED          // "AI 요약을 준비하지 못했어요"
}
