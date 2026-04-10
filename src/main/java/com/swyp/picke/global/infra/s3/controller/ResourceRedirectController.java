package com.swyp.picke.global.infra.s3.controller;

import com.swyp.picke.global.infra.local.service.LocalDraftFileStorageService;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "리소스 리다이렉트 API", description = "공개 리소스 요청을 S3 Presigned URL 또는 로컬 임시파일로 전달")
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceRedirectController {

    private final S3PresignedUrlService s3PresignedUrlService;
    private final LocalDraftFileStorageService localDraftFileStorageService;

    @Operation(summary = "이미지 리소스 리다이렉트")
    @GetMapping("/images/{category}/{fileName:.+}")
    public ResponseEntity<Void> redirectImage(
            @PathVariable FileCategory category,
            @PathVariable String fileName
    ) {
        String objectKey = String.format("%s/%s", category.getPath(), fileName);
        return getRedirectResponse(objectKey);
    }

    @Operation(summary = "시나리오 오디오 리소스 리다이렉트")
    @GetMapping("/audio/scenarios/{battleId}/{fileName:.+}")
    public ResponseEntity<Void> redirectScenarioAudio(
            @PathVariable Long battleId,
            @PathVariable String fileName
    ) {
        String objectKey = String.format("%s/%d/%s",
                FileCategory.SCENARIO.getPath(), battleId, fileName);
        return getRedirectResponse(objectKey);
    }

    @Operation(summary = "로컬 임시 이미지 조회")
    @GetMapping("/local/{fileName:.+}")
    public ResponseEntity<Resource> getLocalDraftImage(@PathVariable String fileName) {
        Resource resource = localDraftFileStorageService.loadDraftResource(fileName);
        return ResponseEntity.ok()
                .contentType(localDraftFileStorageService.resolveMediaType(fileName))
                .cacheControl(CacheControl.noStore())
                .body(resource);
    }

    private ResponseEntity<Void> getRedirectResponse(String objectKey) {
        String presignedUrl = s3PresignedUrlService.generatePresignedUrl(objectKey);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                .build();
    }
}
