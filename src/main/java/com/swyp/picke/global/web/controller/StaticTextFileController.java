package com.swyp.picke.global.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * app-ads.txt, robots.txt는 크롤러(AdMob, 검색엔진)가 Content-Type: text/plain을 기대하므로,
 * 정적 리소스 핸들러의 MIME 추론에 맡기지 않고 명시적으로 text/plain으로 응답한다.
 * 이용약관/개인정보처리방침은 노션 공유 링크가 로그인 리다이렉트 루프에 걸려 외부(AdMob 심사 등)에서
 * 접근 불가능해서, 인증 없이 접근 가능한 정적 HTML로 직접 서빙한다.
 */
@Tag(name = "정적 텍스트 파일", description = "app-ads.txt, robots.txt, 약관/정책 페이지 서빙")
@RestController
public class StaticTextFileController {

    private static final MediaType TEXT_PLAIN_UTF8 = new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8);

    @Operation(summary = "app-ads.txt 조회")
    @GetMapping("/app-ads.txt")
    public ResponseEntity<Resource> getAppAdsTxt() throws IOException {
        return textResponse("static/app-ads.txt");
    }

    @Operation(summary = "robots.txt 조회")
    @GetMapping("/robots.txt")
    public ResponseEntity<Resource> getRobotsTxt() throws IOException {
        return textResponse("static/robots.txt");
    }

    @Operation(summary = "서비스 이용약관 조회")
    @GetMapping("/terms")
    public ResponseEntity<Resource> getTerms() throws IOException {
        return htmlResponse("static/terms.html");
    }

    @Operation(summary = "개인정보 처리방침 조회")
    @GetMapping("/privacy-policy")
    public ResponseEntity<Resource> getPrivacyPolicy() throws IOException {
        return htmlResponse("static/privacy-policy.html");
    }

    private ResponseEntity<Resource> textResponse(String classpathLocation) throws IOException {
        Resource resource = new ClassPathResource(classpathLocation);
        return ResponseEntity.ok()
                .contentType(TEXT_PLAIN_UTF8)
                .contentLength(resource.contentLength())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(resource);
    }

    private ResponseEntity<Resource> htmlResponse(String classpathLocation) throws IOException {
        Resource resource = new ClassPathResource(classpathLocation);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .contentLength(resource.contentLength())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(resource);
    }
}
