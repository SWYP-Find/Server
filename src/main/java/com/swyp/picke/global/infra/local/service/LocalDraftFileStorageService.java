package com.swyp.picke.global.infra.local.service;

import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.service.S3UploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class LocalDraftFileStorageService {

    private static final String LOCAL_DRAFT_PREFIX = "local/drafts/";
    private static final String LOCAL_RESOURCE_PREFIX = "/api/v1/resources/local/";

    @Value("${picke.local-storage.root:${java.io.tmpdir}/picke-local-storage}")
    private String localStorageRoot;

    @Value("${picke.baseUrl}")
    private String baseUrl;

    public String saveDraftFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        String originalName = Optional.ofNullable(multipartFile.getOriginalFilename()).orElse("draft.bin");
        String sanitizedName = sanitizeFileName(originalName);
        String fileName = UUID.randomUUID() + "_" + sanitizedName;
        String localKey = LOCAL_DRAFT_PREFIX + fileName;

        Path targetPath = resolvePath(localKey);
        Files.createDirectories(targetPath.getParent());
        Files.copy(multipartFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return localKey;
    }

    public String normalizeLocalDraftKey(String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            return null;
        }

        String trimmed = rawReference.trim();
        if (trimmed.startsWith(LOCAL_DRAFT_PREFIX)) {
            return trimmed;
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                URI uri = URI.create(trimmed);
                String path = uri.getPath();
                return extractLocalKeyFromPath(path);
            } catch (IllegalArgumentException ignored) {
                return trimmed;
            }
        }

        if (trimmed.startsWith("/")) {
            String localKey = extractLocalKeyFromPath(trimmed);
            return localKey != null ? localKey : trimmed;
        }

        return trimmed;
    }

    public boolean isLocalDraftReference(String rawReference) {
        String normalized = normalizeLocalDraftKey(rawReference);
        return normalized != null && normalized.startsWith(LOCAL_DRAFT_PREFIX);
    }

    public String toPublicUrl(String localKey) {
        if (!isLocalDraftReference(localKey)) {
            return localKey;
        }

        String safeBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return safeBaseUrl + LOCAL_RESOURCE_PREFIX + extractFileName(normalizeLocalDraftKey(localKey));
    }

    public String promoteLocalDraftToS3(String rawReference, FileCategory category, S3UploadService s3UploadService) {
        String normalized = normalizeLocalDraftKey(rawReference);
        if (!isLocalDraftReference(normalized)) {
            return normalized;
        }

        Path localPath = resolvePath(normalized);
        if (!Files.exists(localPath)) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        String fileName = extractFileName(normalized);
        String s3Key = category.getPath() + "/" + fileName;
        s3UploadService.uploadFile(s3Key, localPath.toFile());
        deleteIfLocalReference(normalized);
        return s3Key;
    }

    public void deleteIfLocalReference(String rawReference) {
        String normalized = normalizeLocalDraftKey(rawReference);
        if (!isLocalDraftReference(normalized)) {
            return;
        }

        Path localPath = resolvePath(normalized);
        try {
            Files.deleteIfExists(localPath);
        } catch (IOException ignored) {
            // Draft file cleanup failure should not break content save/update flow.
        }
    }

    public Resource loadDraftResource(String fileName) {
        String sanitized = sanitizeFileName(fileName);
        Path path = resolvePath(LOCAL_DRAFT_PREFIX + sanitized);
        if (!Files.exists(path)) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return new PathResource(path);
    }

    public MediaType resolveMediaType(String fileName) {
        String sanitized = sanitizeFileName(fileName);
        Path path = resolvePath(LOCAL_DRAFT_PREFIX + sanitized);
        try {
            String contentType = Files.probeContentType(path);
            if (contentType != null && !contentType.isBlank()) {
                return MediaType.parseMediaType(contentType);
            }
        } catch (IOException ignored) {
            // fall through
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String extractLocalKeyFromPath(String path) {
        if (path == null) return null;
        int markerIndex = path.indexOf(LOCAL_RESOURCE_PREFIX);
        if (markerIndex < 0) return null;

        String fileName = path.substring(markerIndex + LOCAL_RESOURCE_PREFIX.length());
        if (fileName.isBlank()) return null;

        return LOCAL_DRAFT_PREFIX + sanitizeFileName(fileName);
    }

    private Path resolvePath(String localKey) {
        Path root = Paths.get(localStorageRoot).toAbsolutePath().normalize();
        Path resolved = root.resolve(localKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        return resolved;
    }

    private String extractFileName(String localKey) {
        String normalized = normalizeLocalDraftKey(localKey);
        if (normalized == null || !normalized.startsWith(LOCAL_DRAFT_PREFIX)) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        return normalized.substring(LOCAL_DRAFT_PREFIX.length());
    }

    private String sanitizeFileName(String fileName) {
        return fileName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("..", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
