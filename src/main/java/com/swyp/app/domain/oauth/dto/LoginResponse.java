package com.swyp.app.domain.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 서버가 클라이언트에게 데이터를 돌려줄 때, 데이터를 담는 DTO
@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String userTag; // 회의에서 userTag 반환하는 것으로 통일했기 때문에 userId 대신 userTag 반환

    @JsonProperty("is_new_user")
    private boolean isNewUser;
    private String status;
}