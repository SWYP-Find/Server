package com.swyp.picke.domain.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginRequest {
    // 1. 구글, 카카오용 인가 코드 및 애플용 authorizationCode 공용 필드
    private String authorizationCode;

    // 2. 구글, 카카오용 선택 필드
    private String redirectUri;

    // 3. 애플 로그인에 사용되는 필수 자격 증명 토큰
    private String identityToken;
}
