package com.swyp.picke.domain.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "소셜 로그인 요청 객체")
public class LoginRequest {

    @Schema(
            description = "인가 코드 (구글/카카오 로그인 시 필수, 애플 로그인 시에도 선택 또는 필수 전달)",
            example = "4/0AeanS0..."
    )
    private String authorizationCode;

    @Schema(
            description = "리다이렉트 URI (구글/카카오 로그인 시 사용)",
            example = "https://picke.store/login/callback"
    )
    private String redirectUri;

    @Schema(
            description = "애플 identityToken (애플 로그인 시 필수)",
            example = "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
    )
    private String identityToken;
}