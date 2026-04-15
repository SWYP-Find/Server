package com.swyp.picke.global.infra.s3.controller;

import com.swyp.picke.global.common.response.ApiResponse;
import com.swyp.picke.global.infra.local.service.LocalDraftFileStorageService;
import com.swyp.picke.global.infra.s3.dto.FileUploadResponse;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import com.swyp.picke.global.infra.s3.service.S3UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Tag(name = "파일 업로드 API", description = "관리자 파일 업로드 (S3 / 로컬 임시저장)")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FileUploadController {

    private final S3UploadService s3UploadService;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final LocalDraftFileStorageService localDraftFileStorageService;

    @Operation(summary = "S3 파일 업로드")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> uploadFile(
            @Parameter(description = "업로드 파일", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile multipartFile,
            @Parameter(description = "업로드 카테고리")
            @RequestParam("category") FileCategory category
    ) throws IOException {

        File tempFile = convertMultiPartToFile(multipartFile);
        try {
            String fileName = category.getPath() + "/" + UUID.randomUUID() + "_" + multipartFile.getOriginalFilename();
            String s3Key = s3UploadService.uploadFile(fileName, tempFile);
            String presignedUrl = s3PresignedUrlService.generatePresignedUrl(s3Key);
            return ApiResponse.onSuccess(new FileUploadResponse(s3Key, presignedUrl));
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Operation(summary = "로컬 임시 파일 업로드")
    @PostMapping(value = "/upload/local", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> uploadLocalDraftFile(
            @Parameter(description = "업로드 파일", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile multipartFile
    ) throws IOException {
        String localKey = localDraftFileStorageService.saveDraftFile(multipartFile);
        String localUrl = localDraftFileStorageService.toPublicUrl(localKey);
        return ApiResponse.onSuccess(new FileUploadResponse(localKey, localUrl));
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        String safeName = (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank())
                ? "upload.bin"
                : file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]", "_");

        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + "_" + safeName);
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
