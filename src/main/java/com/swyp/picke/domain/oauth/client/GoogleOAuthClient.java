package com.swyp.picke.domain.oauth.client;

import com.swyp.picke.domain.oauth.dto.OAuthUserInfo;
import com.swyp.picke.domain.oauth.dto.google.GoogleTokenResponse;
import com.swyp.picke.domain.oauth.dto.google.GoogleUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    // 인가 코드 → 구글 access_token
    public String getAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        GoogleTokenResponse response = WebClient.create()
                .post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class)
                .block();

        return response.getAccessToken();
    }

    // 구글 access_token → 사용자 정보
    public OAuthUserInfo getUserInfo(String accessToken) {
        GoogleUserResponse response = WebClient.create()
                .get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(GoogleUserResponse.class)
                .block();

        return new OAuthUserInfo("GOOGLE", response.getId(), response.getEmail());
    }
}
