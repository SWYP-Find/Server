package com.swyp.app.global.infra.s3.controller;

import com.swyp.app.global.common.response.ApiResponse;
import com.swyp.app.global.infra.s3.dto.FileUploadResponse;
import com.swyp.app.global.infra.s3.enums.FileCategory;
import com.swyp.app.global.infra.s3.service.S3PresignedUrlService;
import com.swyp.app.global.infra.s3.service.S3UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Tag(name = "인프라 - 파일 업로드 (File)", description = "S3 파일 업로드 API (Admin 전용)")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FileUploadController {

    private final S3UploadService s3UploadService;
    private final S3PresignedUrlService s3PresignedUrlService;

    @Operation(summary = "S3 파일 업로드", description = "도메인 카테고리(PHILOSOPHER, BATTLE, SCENARIO)에 맞춰 파일을 업로드합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> uploadFile(
            @Parameter(description = "업로드할 파일", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile multipartFile,

            @Parameter(description = "도메인 카테고리 (PHILOSOPHER, BATTLE, SCENARIO)")
            @RequestParam("category") FileCategory category) throws IOException {

        // 1. MultipartFile -> Local File 변환
        File tempFile = convertMultiPartToFile(multipartFile);

        // 2. 경로 생성 (예: images/battles/UUID_thumb.png)
        String fileName = category.getPath() + "/" + UUID.randomUUID() + "_" + multipartFile.getOriginalFilename();

        // 3. S3 업로드 (S3 키 반환)
        String s3Key = s3UploadService.uploadFile(fileName, tempFile);

        // 4. 미리보기용 Presigned URL 생성
        String presignedUrl = s3PresignedUrlService.generatePresignedUrl(s3Key);

        return ApiResponse.onSuccess(new FileUploadResponse(s3Key, presignedUrl));
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}