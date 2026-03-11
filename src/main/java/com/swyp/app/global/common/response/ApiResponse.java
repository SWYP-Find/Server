package com.swyp.app.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"statusCode", "message", "data", "error"})
public class ApiResponse<T> {

    private final int statusCode;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final ErrorResponse error;

    // 성공 응답 (기본)
    public static <T> ApiResponse<T> onSuccess(T data) {
        return new ApiResponse<>(200, "요청에 성공하였습니다.", data, null);
    }

    // 성공 응답 (메시지 커스텀)
    public static <T> ApiResponse<T> onSuccess(String message, T data) {
        return new ApiResponse<>(200, message, data, null);
    }

    // 에러 응답
    public static ApiResponse<Void> onFailure(int statusCode, String errorCode, String message) {
        return new ApiResponse<>(statusCode, message, null, new ErrorResponse(errorCode, message));
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private final String code;
        private final String message;
    }
}