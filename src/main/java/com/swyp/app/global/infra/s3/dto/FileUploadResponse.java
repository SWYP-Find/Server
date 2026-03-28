package com.swyp.app.global.infra.s3.dto;

// TODO: S3 Presigned URL 정식 구현 시 교체 필요 (임시 구현)
public record FileUploadResponse(String s3Key, String presignedUrl) {}
