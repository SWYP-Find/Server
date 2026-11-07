package com.swyp.picke.global.common.exception;

import com.swyp.picke.global.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException; // 추가
import org.springframework.security.authorization.AuthorizationDeniedException; // 추가
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.sql.SQLException;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        boolean isContentSizeViolation = e.getBindingResult().getFieldErrors().stream()
                .anyMatch(fe -> "content".equals(fe.getField()) && "Size".equals(fe.getCode()));
        String requestType = e.getParameter().getParameterType().getName();
        ErrorCode code;
        if (isContentSizeViolation) {
            code = ErrorCode.PERSPECTIVE_CONTENT_TOO_LONG;
        } else if (requestType.startsWith("com.swyp.picke.domain.funding.")) {
            String invalidField = e.getBindingResult().getFieldErrors().stream()
                    .map(org.springframework.validation.FieldError::getField).findFirst().orElse("");
            code = switch (invalidField) {
                case "capacity" -> ErrorCode.FUNDING_REWARD_UNSUPPORTED;
                case "status", "channel" -> ErrorCode.FUNDING_INVALID_DELIVERY_STATUS;
                default -> ErrorCode.FUNDING_INVALID_REQUEST;
            };
        } else {
            code = ErrorCode.COMMON_INVALID_PARAMETER;
        }
        log.warn("Validation failed: {}", e.getMessage());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.onFailure(code.getHttpStatus().value(), code.getCode(), code.getMessage()));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        ErrorCode code = ErrorCode.COMMON_REQUEST_TOO_LARGE;
        return ResponseEntity.status(code.getHttpStatus())
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String detail = String.valueOf(e.getMostSpecificCause().getMessage());
        String normalizedDetail = detail.toLowerCase(java.util.Locale.ROOT);
        String sqlState = findSqlState(e);
        ErrorCode code = normalizedDetail.contains("uk_tumblbug_supporter_line")
                || normalizedDetail.contains("uk_tumblbug_funding_order")
                || normalizedDetail.contains("uk_funding_orders_order_id")
                        ? ErrorCode.FUNDING_TUMBLBUG_IMPORT_DUPLICATE
                        : normalizedDetail.contains("uk_funding_campaign_platform_external")
                                ? ErrorCode.FUNDING_CAMPAIGN_DUPLICATE
                                : isBadRequestConstraint(sqlState)
                                        ? ErrorCode.COMMON_INVALID_PARAMETER
                                        : ErrorCode.COMMON_CONFLICT;
        log.warn("Data integrity constraint rejected a request: sqlState={}", sqlState);
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.onFailure(code.getHttpStatus().value(), code.getCode(), code.getMessage()));
    }

    private String findSqlState(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                return sqlException.getSQLState();
            }
            current = current.getCause();
        }
        return null;
    }

    private boolean isBadRequestConstraint(String sqlState) {
        return "23502".equals(sqlState) // not-null
                || "23514".equals(sqlState) // check
                || "22001".equals(sqlState); // value too long
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
