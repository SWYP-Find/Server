package com.swyp.picke.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"statusCode", "data", "error"})
public class ApiResponse<T> {

    private final int statusCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final ErrorResponse error;

    // 성공 응답
    public static <T> ApiResponse<T> onSuccess(T data) {
        return new ApiResponse<>(200, data, null);
    }

    // 에러 응답
    public static ApiResponse<Void> onFailure(int statusCode, String errorCode, String message) {
        return new ApiResponse<>(statusCode, null, new ErrorResponse(errorCode, message));
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private final String code;
        private final String message;
    }
}