package com.swyp.picke.domain.admin.analytics;

public enum AnalyticsStatus {
    /** 토큰·프로젝트 설정이 없다. 지표를 비워 보여주고 오류로 다루지 않는다. */
    NOT_CONFIGURED,
    CONNECTED,
    /** 토큰 거절, 외부 장애, 응답 스키마 불일치. 0으로 대체하지 않는다. */
    UNAVAILABLE
}
