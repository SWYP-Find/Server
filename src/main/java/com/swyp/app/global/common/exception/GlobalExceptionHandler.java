package com.swyp.app.global.common.exception;

import com.swyp.app.global.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException; // 추가
import org.springframework.security.authorization.AuthorizationDeniedException; // 추가
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.onFailure(code.getHttpStatus().value(), code.getCode(), code.getMessage()));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.warn("Bad Request: {}", e.getMessage());
        ErrorCode code = ErrorCode.COMMON_INVALID_PARAMETER;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.onFailure(code.getHttpStatus().value(), code.getCode(), code.getMessage()));
    }

    @ExceptionHandler({
            AuthorizationDeniedException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(Exception e) {
        log.warn("Access Denied (권한 없음): {}", e.getMessage());
        ErrorCode code = ErrorCode.AUTH_FORBIDDEN;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.onFailure(code.getHttpStatus().value(), code.getCode(), code.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllException(Exception e) {
        log.error("Internal Server Error: ", e);
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.onFailure(500, code.getCode(), code.getMessage()));
    }
}