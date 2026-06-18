package com.swyp.picke.domain.oauth.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.oauth.dto.OAuthUserInfo;
import com.swyp.picke.domain.oauth.dto.apple.ApplePublicKeyResponse;
import com.swyp.picke.domain.oauth.dto.apple.AppleTokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleOAuthClient {

    @Value("${oauth.apple.team-id}")
    private String teamId;

    @Value("${oauth.apple.client-id}")
    private String clientId;

    @Value("${oauth.apple.key-id}")
    private String keyId;

    @Value("${oauth.apple.private-key}")
    private String privateKeyStr;

    public OAuthUserInfo getUserInfo(String identityToken) {
        try {
            // 1. 애플 실시간 공개키 목록 조회
            ApplePublicKeyResponse publicKeyResponse = WebClient.create()
                    .get()
                    .uri("https://appleid.apple.com/auth/keys")
                    .retrieve()
                    .bodyToMono(ApplePublicKeyResponse.class)
                    .block();

            // 2. identityToken 헤더에서 kid와 alg 추출
            String[] chunks = identityToken.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(chunks[0]));
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> header = mapper.readValue(headerJson, Map.class);
            String kid = header.get("kid");
            String alg = header.get("alg");

            // 3. 일치하는 애플 공개키 매핑
            ApplePublicKeyResponse.Key matchedKey = publicKeyResponse.getKeys().stream()
                    .filter(key -> key.getKid().equals(kid) && key.getAlg().equals(alg))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("일치하는 애플 공개키를 찾을 수 없습니다."));

            // 4. RSA 공개키 인스턴스 생성
            byte[] nBytes = Base64.getUrlDecoder().decode(matchedKey.getN());
            byte[] eBytes = Base64.getUrlDecoder().decode(matchedKey.getE());

            BigInteger n = new BigInteger(1, nBytes);
            BigInteger e = new BigInteger(1, eBytes);

            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
            KeyFactory keyFactory = KeyFactory.getInstance(matchedKey.getKty());
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            // 5. JJWT 0.12.6 버전에 맞춘 최신 빌더 문법으로 토큰 검증 및 파싱
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey) // parserBuilder() 및 setSigningKey() 대신 사용
                    .build()
                    .parseSignedClaims(identityToken) // parseClaimsJws() 대신 사용
                    .getPayload(); // getBody() 대신 사용

            // 6. 대상 오디언스(Bundle ID)가 맞는지 수동 검증 보강
            if (!clientId.equals(claims.getAudience().iterator().next())) {
                throw new RuntimeException("애플 토큰의 Audience가 올바르지 않습니다.");
            }

            return new OAuthUserInfo("APPLE", claims.getSubject(), claims.get("email", String.class));
        } catch (Exception e) {
            log.error("[Apple Auth Error] 토큰 검증 실패", e);
            throw new RuntimeException("애플 토큰 검증 실패");
        }
    }

    public String getAppleRefreshToken(String code) {
        try {
            String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
            String clientSecret = createClientSecret();

            log.info("[Apple Login] 리프레시 토큰 발급 요청 시작");

            AppleTokenResponse response = WebClient.create()
                    .post()
                    .uri("https://appleid.apple.com/auth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                                  .with("client_id", clientId)
                                  .with("client_secret", clientSecret)
                                  .with("code", decodedCode))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("[Apple Token Error] 상세 내용: {}", errorBody);
                                return Mono.error(new RuntimeException("애플 토큰 발급 실패"));
                            })
                    )
                    .bodyToMono(AppleTokenResponse.class)
                    .block();

            return response != null ? response.getRefreshToken() : null;
        } catch (Exception e) {
            log.error("[Apple Refresh Token Error] 발급 실패", e);
            throw new RuntimeException("애플 리프레시 토큰 획득 실패");
        }
    }

    private String createClientSecret() {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 3600000);

        // 7. JJWT 0.12.6 버전에 맞춘 동적 Secret 생성 서명 문법 적용
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .issuedAt(now)
                .expiration(expiration)
                .audience().add("https://appleid.apple.com").and()
                .subject(clientId)
                .signWith(getPrivateKey(), Jwts.SIG.ES256) // SignatureAlgorithm.ES256 대신 사용
                .compact();
    }

    public void revokeAppleAccount(String appleRefreshToken) {
        try {
            String clientSecret = createClientSecret();

            log.info("[Apple Withdrawal] 연동 해제(Revoke) 요청 시작");

            WebClient.create()
                    .post()
                    .uri("https://appleid.apple.com/auth/revoke")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", clientId)
                                  .with("client_secret", clientSecret)
                                  .with("token", appleRefreshToken)
                                  .with("token_type_hint", "refresh_token"))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("[Apple Revoke Error] 상세 내용: {}", errorBody);
                                return Mono.error(new RuntimeException("애플 연동 해제 실패"));
                            })
                    )
                    .toBodilessEntity() // 애플 응답 스펙인 200 OK 빈 바디 수신 처리
                    .block();

            log.info("[Apple Withdrawal] 연동 해제(Revoke) 완료 안내 메일 발송 트리거 성공");
        } catch (Exception e) {
            log.error("[Apple Revoke Error] 연동 해제 처리 실패", e);
            throw new RuntimeException("애플 회원 연동 해제 처리 실패");
        }
    }

    private PrivateKey getPrivateKey() {
        try {
            String pem = privateKeyStr.replace("\\n", "\n");
            PEMParser pemParser = new PEMParser(new StringReader(pem));
            PrivateKeyInfo object = (PrivateKeyInfo) pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            return converter.getPrivateKey(object);
        } catch (Exception e) {
            log.error("[Apple PrivateKey Error] 키 생성 실패", e);
            throw new RuntimeException("애플 비밀키 생성 실패");
        }
    }
}