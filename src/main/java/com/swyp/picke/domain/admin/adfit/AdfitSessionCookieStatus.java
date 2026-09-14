package com.swyp.picke.domain.admin.adfit;

import java.time.LocalDateTime;

/**
 * 쿠키 보유 여부와 마지막 갱신 시각. 쿠키 값 자체는 담지 않는다.
 *
 * @param updatedAt 관리자가 마지막으로 넣은 시각. 환경변수 값을 쓰는 중이면 null.
 */
public record AdfitSessionCookieStatus(
        boolean configured,
        AdfitSessionCookieSource source,
        LocalDateTime updatedAt) {
}
