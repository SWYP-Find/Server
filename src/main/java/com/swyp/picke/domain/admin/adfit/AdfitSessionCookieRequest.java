package com.swyp.picke.domain.admin.adfit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param cookie AdFit 콘솔에 로그인한 브라우저의 Cookie 헤더 전체. {@code name=value; name2=value2} 형태.
 */
public record AdfitSessionCookieRequest(
        @NotBlank @Size(max = 4096) String cookie) {
}
