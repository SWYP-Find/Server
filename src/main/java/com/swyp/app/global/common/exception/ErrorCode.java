package com.swyp.app.global.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 에러, 관리자에게 문의하세요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),

    // Battle & Tag
    BATTLE_NOT_FOUND(HttpStatus.NOT_FOUND, "BATTLE_404", "존재하지 않는 배틀입니다."),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "TAG_404", "존재하지 않는 태그입니다."),

    // Auth ( Token )
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
    AUTH_INVALID_CODE(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CODE", "유효하지 않은 소셜 인가 코드입니다."),
    AUTH_ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_ACCESS_TOKEN_EXPIRED", "Access Token이 만료되었습니다."),
    AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_EXPIRED", "Refresh Token이 만료되었습니다. 다시 로그인이 필요합니다."),

    // OAuth ( Social Login )
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "INVALID_PROVIDER", "지원하지 않는 소셜 로그인 provider입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}