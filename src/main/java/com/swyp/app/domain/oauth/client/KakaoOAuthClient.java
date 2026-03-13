package com.swyp.app.domain.oauth.client;

import com.swyp.app.domain.oauth.dto.OAuthUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    // @Value("${oauth.kakao.client-secret}")
    // private String clientSecret;

    // 인가 코드 → 카카오 access_token
    public String getAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        // 카카오 보안 코드는 아직 활성화가 되지 않아서 클라이언트 시크릿 키는 제외
        // body.add("client_secret", clientSecret;
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        Map response = WebClient.create()
                .post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }

    // 카카오 access_token → 사용자 정보
    public OAuthUserInfo getUserInfo(String accessToken) {
        Map response = WebClient.create()
                .get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String providerId = String.valueOf(response.get("id"));

        return new OAuthUserInfo("KAKAO", providerId, null);
    }
}