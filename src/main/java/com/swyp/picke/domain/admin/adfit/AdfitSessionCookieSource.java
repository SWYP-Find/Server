package com.swyp.picke.domain.admin.adfit;

public enum AdfitSessionCookieSource {
    /** 관리자가 화면에서 넣은 값. 환경변수보다 앞선다. */
    ADMIN_CONSOLE,
    /** 배포 시 주입한 환경변수 값. */
    ENVIRONMENT,
    /** 양쪽 모두 없음. 자동 수익 조회가 NOT_CONFIGURED 가 된다. */
    NONE
}
