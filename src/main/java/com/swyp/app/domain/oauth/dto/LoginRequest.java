package com.swyp.app.domain.oauth.dto;

import lombok.Getter;

// 클라이언트가 서버로 요청을 보낼 때, 데이터를 담는 DTO
@Getter
public class LoginRequest {
    private String authorizationCode;
    private String redirectUri;
}
