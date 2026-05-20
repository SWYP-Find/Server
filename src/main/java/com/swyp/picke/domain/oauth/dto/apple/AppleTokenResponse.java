package com.swyp.picke.domain.oauth.dto.apple;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AppleTokenResponse {
    private String accessToken;
    private String expiresIn;
    private String idToken;
    private String refreshToken;
    private String tokenType;
}