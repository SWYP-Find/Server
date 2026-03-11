package com.swyp.app.global.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Common
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 에러, 관리자에게 문의하세요."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),

    // Battle & Tag
    BATTLE_NOT_FOUND(HttpStatus.NOT_FOUND, "BATTLE_404", "존재하지 않는 배틀입니다."),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "TAG_404", "존재하지 않는 태그입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}