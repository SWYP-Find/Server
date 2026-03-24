package com.swyp.app.domain.oauth.controller;

import com.swyp.app.domain.oauth.dto.LoginRequest;
import com.swyp.app.domain.oauth.dto.LoginResponse;
import com.swyp.app.domain.oauth.dto.LogoutResponse;
import com.swyp.app.domain.oauth.dto.WithdrawResponse;
import com.swyp.app.domain.oauth.service.AuthService;
import com.swyp.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "인증", description = "인증 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "소셜 로그인")
    @PostMapping("/auth/login/{provider}")
    public ApiResponse<LoginResponse> login(
            @PathVariable String provider,
            @RequestBody LoginRequest request
    ) {
        return ApiResponse.onSuccess(authService.login(provider, request));
    }

    @Operation(summary = "Access Token 재발급")
    @PostMapping("/auth/refresh")
    public ApiResponse<LoginResponse> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken
    ) {
        return ApiResponse.onSuccess(authService.refresh(refreshToken));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/auth/logout")
    public ApiResponse<LogoutResponse> logout(
            @AuthenticationPrincipal Long userId
    ) {
        authService.logout(userId);
        return ApiResponse.onSuccess(new LogoutResponse(true));
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ApiResponse<WithdrawResponse> withdraw(
            @AuthenticationPrincipal Long userId
    ) {
        authService.withdraw(userId);
        return ApiResponse.onSuccess(new WithdrawResponse(true));
    }
}