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
 */
@Tag(name = "정적 텍스트 파일", description = "app-ads.txt, robots.txt 서빙")
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

    private ResponseEntity<Resource> textResponse(String classpathLocation) throws IOException {
        Resource resource = new ClassPathResource(classpathLocation);
        return ResponseEntity.ok()
                .contentType(TEXT_PLAIN_UTF8)
                .contentLength(resource.contentLength())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(resource);
    }
}
