package com.swyp.app.global.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 에러, 관리자에게 문의하세요."),
    COMMON_INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON_INVALID_PARAMETER", "요청 파라미터가 잘못되었습니다."),

    // Auth (Token)
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
    AUTH_INVALID_CODE(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CODE", "유효하지 않은 소셜 인가 코드입니다."),
    AUTH_ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_ACCESS_TOKEN_EXPIRED", "Access Token이 만료되었습니다."),
    AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_EXPIRED", "Refresh Token이 만료되었습니다. 다시 로그인이 필요합니다."),

    // User
    USER_BANNED(HttpStatus.FORBIDDEN, "USER_BANNED", "영구 제재된 사용자입니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "USER_SUSPENDED", "일정 기간 이용 정지된 사용자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "ONBOARDING_ALREADY_COMPLETED", "이미 온보딩이 완료된 사용자입니다."),

    // OAuth (Social Login)
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "INVALID_PROVIDER", "지원하지 않는 소셜 로그인 provider입니다."),

    // Battle & Tag
    BATTLE_NOT_FOUND(HttpStatus.NOT_FOUND, "BATTLE_NOT_FOUND", "존재하지 않는 배틀입니다."),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "TAG_NOT_FOUND", "존재하지 않는 태그입니다."),

    // Perspective
    PERSPECTIVE_NOT_FOUND(HttpStatus.NOT_FOUND, "PERSPECTIVE_NOT_FOUND", "존재하지 않는 관점입니다."),
    PERSPECTIVE_ALREADY_EXISTS(HttpStatus.CONFLICT, "PERSPECTIVE_ALREADY_EXISTS", "이미 관점을 작성한 배틀입니다."),
    PERSPECTIVE_FORBIDDEN(HttpStatus.FORBIDDEN, "PERSPECTIVE_FORBIDDEN", "본인 관점만 수정/삭제할 수 있습니다."),
    PERSPECTIVE_POST_VOTE_REQUIRED(HttpStatus.CONFLICT, "PERSPECTIVE_POST_VOTE_REQUIRED", "사후 투표가 완료되지 않았습니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "존재하지 않는 댓글입니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN", "본인 댓글만 수정/삭제할 수 있습니다."),

    // Like
    LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "LIKE_ALREADY_EXISTS", "이미 좋아요를 누른 관점입니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "LIKE_NOT_FOUND", "좋아요를 누른 적 없는 관점입니다."),
    LIKE_SELF_FORBIDDEN(HttpStatus.FORBIDDEN, "LIKE_SELF_FORBIDDEN", "본인 관점에는 좋아요를 누를 수 없습니다."),

    // Vote
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "VOTE_NOT_FOUND", "투표 내역이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}