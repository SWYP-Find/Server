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
    COMMON_INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON_400", "요청 파라미터가 잘못되었습니다."),

    // File & S3
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_500", "파일 업로드 중 오류가 발생했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE_400_SIZE", "파일 용량이 제한(10MB)을 초과하였습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_404", "업로드할 파일이 존재하지 않습니다."),
    FILE_CONVERSION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_500_CONV", "파일 변환 중 오류가 발생했습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "FILE_400_EXT", "지원하지 않는 파일 확장자입니다."),

    // Auth (Token)
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
    AUTH_INVALID_CODE(HttpStatus.UNAUTHORIZED, "AUTH_401_CODE", "유효하지 않은 소셜 인가 코드입니다."),
    AUTH_ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_ACCESS", "Access Token이 만료되었습니다."),
    AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_REFRESH", "Refresh Token이 만료되었습니다. 다시 로그인이 필요합니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "해당 API에 대한 접근 권한(관리자 등)이 없습니다."),

    // User
    USER_BANNED(HttpStatus.FORBIDDEN, "USER_403_BAN", "영구 제재된 사용자입니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "USER_403_SUS", "일정 기간 이용 정지된 사용자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "존재하지 않는 사용자입니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "USER_409", "이미 온보딩이 완료된 사용자입니다."),

    // OAuth (Social Login)
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH_400_PROVIDER", "지원하지 않는 소셜 로그인 provider입니다."),

    // Notice
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_404", "존재하지 않는 공지사항입니다."),

    // Battle
    BATTLE_NOT_FOUND        (HttpStatus.NOT_FOUND, "BATTLE_404",     "존재하지 않는 배틀입니다."),
    BATTLE_CLOSED           (HttpStatus.CONFLICT,  "BATTLE_409_CLS", "종료된 배틀입니다."),
    BATTLE_ALREADY_PUBLISHED(HttpStatus.CONFLICT,  "BATTLE_409_PUB", "이미 발행된 배틀입니다."),
    BATTLE_OPTION_NOT_FOUND (HttpStatus.NOT_FOUND, "BATTLE_OPT_404", "존재하지 않는 선택지입니다."),
    BATTLE_INVALID_OPTION_COUNT(HttpStatus.BAD_REQUEST, "BATTLE_400_OPT", "배틀 타입에 맞지 않는 선택지 개수입니다."),

    // Scenario
    SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "SCENARIO_404", "존재하지 않는 시나리오입니다."),
    SCENARIO_ALREADY_EXISTS(HttpStatus.CONFLICT, "SCENARIO_409_DUP", "해당 배틀에 이미 시나리오가 존재합니다."),
    SCENARIO_ALREADY_PUBLISHED(HttpStatus.CONFLICT, "SCENARIO_409_PUB", "이미 발행된 시나리오는 수정할 수 없습니다."),

    // Tag
    TAG_NOT_FOUND     (HttpStatus.NOT_FOUND,   "TAG_404",       "존재하지 않는 태그입니다."),
    TAG_DUPLICATED    (HttpStatus.CONFLICT,    "TAG_409_DUP",   "이미 존재하는 태그명입니다."),
    TAG_IN_USE        (HttpStatus.CONFLICT,    "TAG_409_USE",   "배틀에 사용 중인 태그라 삭제할 수 없습니다."),
    TAG_INVALID_ID    (HttpStatus.BAD_REQUEST, "TAG_400_ID",    "잘못된 태그 ID 형식입니다."),
    TAG_INVALID_TYPE  (HttpStatus.BAD_REQUEST, "TAG_400_TYPE",  "알 수 없는 태그 타입입니다."),
    TAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "TAG_400_LIMIT", "배틀당 태그 최대 개수를 초과했습니다."),

    // Perspective
    PERSPECTIVE_NOT_FOUND             (HttpStatus.NOT_FOUND,  "PERSPECTIVE_404",      "존재하지 않는 관점입니다."),
    PERSPECTIVE_ALREADY_EXISTS        (HttpStatus.CONFLICT,   "PERSPECTIVE_409",      "이미 관점을 작성한 배틀입니다."),
    PERSPECTIVE_FORBIDDEN             (HttpStatus.FORBIDDEN,  "PERSPECTIVE_403",      "본인 관점만 수정/삭제할 수 있습니다."),
    PERSPECTIVE_POST_VOTE_REQUIRED    (HttpStatus.CONFLICT,   "PERSPECTIVE_VOTE_409", "사후 투표가 완료되지 않았습니다."),
    PERSPECTIVE_MODERATION_NOT_FAILED (HttpStatus.BAD_REQUEST,"PERSPECTIVE_400",      "검수 실패 상태의 관점이 아닙니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "존재하지 않는 댓글입니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN", "본인 댓글만 수정/삭제할 수 있습니다."),

    // Like
    LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT,  "LIKE_409", "이미 좋아요를 누른 관점입니다."),
    LIKE_NOT_FOUND     (HttpStatus.NOT_FOUND, "LIKE_404", "좋아요를 누른 적 없는 관점입니다."),
    LIKE_SELF_FORBIDDEN(HttpStatus.FORBIDDEN, "LIKE_403", "본인 관점에는 좋아요를 누를 수 없습니다."),

    // Vote
    VOTE_NOT_FOUND        (HttpStatus.NOT_FOUND,   "VOTE_404",     "투표 내역이 없습니다."),
    VOTE_ALREADY_SUBMITTED(HttpStatus.CONFLICT,    "VOTE_409_SUB", "이미 투표가 완료되었습니다."),
    INVALID_VOTE_STATUS   (HttpStatus.BAD_REQUEST, "VOTE_400_INV", "사전 투표를 진행해야 하거나, 이미 사후 투표가 완료되었습니다."),
    PRE_VOTE_REQUIRED     (HttpStatus.CONFLICT,    "VOTE_409_PRE", "사전 투표가 필요합니다."),
    POST_VOTE_REQUIRED    (HttpStatus.CONFLICT,    "VOTE_409_PST", "사후 투표가 필요합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
