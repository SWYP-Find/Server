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
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증 정보가 필요합니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "존재하지 않는 유저입니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "USER_409", "이미 온보딩이 완료된 사용자입니다."),

    // Battle & Tag
    BATTLE_NOT_FOUND(HttpStatus.NOT_FOUND, "BATTLE_404", "존재하지 않는 배틀입니다."),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "TAG_404", "존재하지 않는 태그입니다."),

    // Perspective
    PERSPECTIVE_NOT_FOUND(HttpStatus.NOT_FOUND, "PERSPECTIVE_404", "존재하지 않는 관점입니다."),
    PERSPECTIVE_ALREADY_EXISTS(HttpStatus.CONFLICT, "PERSPECTIVE_409", "이미 관점을 작성한 배틀입니다."),
    PERSPECTIVE_FORBIDDEN(HttpStatus.FORBIDDEN, "PERSPECTIVE_403", "본인 관점만 수정/삭제할 수 있습니다."),
    PERSPECTIVE_POST_VOTE_REQUIRED(HttpStatus.CONFLICT, "PERSPECTIVE_VOTE_409", "사후 투표가 완료되지 않았습니다."),
    PERSPECTIVE_MODERATION_NOT_FAILED(HttpStatus.BAD_REQUEST, "PERSPECTIVE_400", "검수 실패 상태의 관점이 아닙니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_404", "존재하지 않는 댓글입니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT_403", "본인 댓글만 수정/삭제할 수 있습니다."),

    // Like
    LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "LIKE_409", "이미 좋아요를 누른 관점입니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "LIKE_404", "좋아요를 누른 적 없는 관점입니다."),
    LIKE_SELF_FORBIDDEN(HttpStatus.FORBIDDEN, "LIKE_403", "본인 관점에는 좋아요를 누를 수 없습니다."),

    // Vote
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "VOTE_404", "투표 내역이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
