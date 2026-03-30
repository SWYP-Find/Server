package com.swyp.picke.global.infra.s3.controller;

import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "인프라 - 리소스 서빙", description = "S3 리다이렉트 API (Public)")
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceRedirectController {

    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * 1. Public 이미지 리다이렉트
     * URL 구조: /api/v1/resources/images/BATTLE/파일명.png
     */
    @Operation(summary = "이미지 리소스 리다이렉트 (Public)")
    @GetMapping("/images/{category}/{fileName:.+}")
    public ResponseEntity<Void> redirectImage(
            @PathVariable FileCategory category,
            @PathVariable String fileName) {

        // category.getPath()를 통해 S3 실제 경로(images/battles 등) 조립
        String objectKey = String.format("%s/%s", category.getPath(), fileName);
        return getRedirectResponse(objectKey);
    }

    /**
     * 2. Public 오디오 리다이렉트
     * URL 구조: /api/v1/resources/audio/scenarios/17/파일명.mp3
     */
    @Operation(summary = "오디오 리소스 리다이렉트 (Public)")
    @GetMapping("/audio/scenarios/{battleId}/{fileName:.+}")
    public ResponseEntity<Void> redirectScenarioAudio(
            @PathVariable Long battleId,
            @PathVariable String fileName) {

        // FileCategory.SCENARIO.getPath() -> "audio/scenarios"
        String objectKey = String.format("%s/%d/%s",
                FileCategory.SCENARIO.getPath(), battleId, fileName);

        return getRedirectResponse(objectKey);
    }

    /**
     * S3 Presigned URL 생성 및 리다이렉트 응답 반환
     */
    private ResponseEntity<Void> getRedirectResponse(String objectKey) {
        String presignedUrl = s3PresignedUrlService.generatePresignedUrl(objectKey);
        return ResponseEntity.status(HttpStatus.FOUND) // 302 Found
                .location(URI.create(presignedUrl))
                .build();
    }
}