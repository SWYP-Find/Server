package com.swyp.picke.domain.oauth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 서버가 클라이언트에게 데이터를 돌려줄 때, 데이터를 담는 DTO
@Getter
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String userTag;
    private boolean isNewUser;
    private String status;
}