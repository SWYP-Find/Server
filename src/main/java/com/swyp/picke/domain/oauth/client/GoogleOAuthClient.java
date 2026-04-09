package com.swyp.picke.domain.oauth.client;

import com.swyp.picke.domain.oauth.dto.OAuthUserInfo;
import com.swyp.picke.domain.oauth.dto.google.GoogleTokenResponse;
import com.swyp.picke.domain.oauth.dto.google.GoogleUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 1. 로그 추가
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters; // 2. 추가
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    // 인가 코드 → 구글 access_token
    public String getAccessToken(String code, String redirectUri) {
        // 3. 인코딩된 코드가 들어올 경우를 대비해 디코딩 처리
        String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);

        log.info("[Google Login] 요청 시작 - redirectUri: {}, code: {}", redirectUri, decodedCode);

        GoogleTokenResponse response = WebClient.create()
                .post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                // 4. BodyInserters를 사용하여 데이터 전송 (가장 안전한 방식)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                              .with("client_id", clientId)
                              .with("client_secret", clientSecret)
                              .with("redirect_uri", redirectUri)
                              .with("code", decodedCode))
                .retrieve()
                // 5. 400 Bad Request 발생 시 구글이 보내는 진짜 이유를 로그로 확인
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("[Google Auth Error] 상세 내용: {}", errorBody);
                            return Mono.error(new RuntimeException("구글 토큰 발급 실패"));
                        })
                )
                .bodyToMono(GoogleTokenResponse.class)
                .block();

        return response != null ? response.getAccessToken() : null;
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