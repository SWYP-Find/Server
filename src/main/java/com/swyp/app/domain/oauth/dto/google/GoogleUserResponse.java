package com.swyp.app.domain.oauth.dto.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class GoogleUserResponse {

    private String id;
    private String email;
    private String name;

    @JsonProperty("verified_email")
    private boolean verifiedEmail;
}
