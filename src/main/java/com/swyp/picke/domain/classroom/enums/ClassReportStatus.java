package com.swyp.picke.domain.classroom.enums;

public enum ClassReportStatus {
    PENDING,
    GENERATING,
    SUCCEEDED,
    LIMITED,   // 작성 글이 없어 서버 템플릿으로 대체, AI 호출 없음
    FAILED
}
