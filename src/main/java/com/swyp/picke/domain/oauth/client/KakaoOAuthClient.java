package com.swyp.picke.domain.oauth.client;

import com.swyp.picke.domain.oauth.dto.OAuthUserInfo;
import com.swyp.picke.domain.oauth.dto.kakao.KakaoTokenResponse;
import com.swyp.picke.domain.oauth.dto.kakao.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret:}")
    private String clientSecret;

    public String getAccessToken(String code, String redirectUri) {
        // 인코딩된 코드가 들어올 경우를 대비해 디코딩 처리
        String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);

        log.info("[Kakao Login] 토큰 요청 시작 - redirectUri: {}, code: {}", redirectUri, decodedCode);

        KakaoTokenResponse response = WebClient.create()
                .post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                // BodyInserters를 사용하여 폼 데이터 전송 (이중 인코딩 방지)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                              .with("client_id", clientId)
                              .with("redirect_uri", redirectUri)
                              .with("code", decodedCode)
                              .with("client_secret", clientSecret)) // 빈 문자열이어도 카카오는 허용함
                .retrieve()
                // 400 에러 발생 시 상세 이유 로그 출력
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("[Kakao Auth Error] 상세 내용: {}", errorBody);
                            return Mono.error(new RuntimeException("카카오 토큰 발급 실패"));
                        })
                )
                .bodyToMono(KakaoTokenResponse.class)
                .block();

        return response != null ? response.getAccessToken() : null;
    }

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