package com.swyp.picke.domain.oauth.client;

import com.swyp.picke.domain.oauth.dto.OAuthUserInfo;
import com.swyp.picke.domain.oauth.dto.kakao.KakaoTokenResponse;
import com.swyp.picke.domain.oauth.dto.kakao.KakaoUserResponse;
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
public class KakaoOAuthClient {

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret:}")
    private String clientSecret;

    // 인가 코드 → 카카오 access_token
    public String getAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        if (clientSecret != null && !clientSecret.isEmpty()) {
            body.add("client_secret", clientSecret);
        }

        KakaoTokenResponse response = WebClient.create()
                .post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block();

        return response.getAccessToken();
    }

    // 카카오 access_token → 사용자 정보
    public OAuthUserInfo getUserInfo(String accessToken) {
        KakaoUserResponse response = WebClient.create()
                .get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .block();

        String providerId = String.valueOf(response.getId());

        return new OAuthUserInfo("KAKAO", providerId, null);
    }
}