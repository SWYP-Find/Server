package com.swyp.picke.domain.classroom.enums;

public enum ClassReportStatus {
    PENDING,
    GENERATING,
    SUCCEEDED,
    LIMITED,   // 작성 글이 없어 서버 템플릿으로 대체, AI 호출 없음
    FAILED,
    CANCELED   // 클래스가 삭제됐거나 멤버가 나가기·내보내기 되어 생성하지 않음. 재시도 대상 아님 (PRD F16·§18)
}
