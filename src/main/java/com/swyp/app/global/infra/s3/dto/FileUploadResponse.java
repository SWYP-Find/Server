package com.swyp.app.global.infra.s3.dto;

public record FileUploadResponse(String s3Key, String presignedUrl) {}
