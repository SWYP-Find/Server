package com.swyp.picke.global.infra.s3.util;

import com.swyp.picke.global.infra.s3.enums.FileCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResourceUrlProvider {

    @Value("${picke.baseUrl}")
    private String baseUrl;

    public String getImageUrl(FileCategory category, String storedKey) {
        if (storedKey == null || storedKey.isBlank()) {
            return null;
        }

        String safeBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String trimmed = storedKey.trim();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        if (trimmed.startsWith("/api/v1/resources/local/") || trimmed.startsWith("/api/v1/resources/images/")) {
            return safeBaseUrl + trimmed;
        }

        if (trimmed.startsWith("local/drafts/")) {
            String fileName = trimmed.substring(trimmed.lastIndexOf("/") + 1);
            return String.format("%s/api/v1/resources/local/%s", safeBaseUrl, fileName);
        }

        String fileName = trimmed.substring(trimmed.lastIndexOf("/") + 1);
        return String.format("%s/api/v1/resources/images/%s/%s",
                safeBaseUrl, category.name(), fileName);
    }

    public String getAudioUrl(Long battleId, String dbFilePath) {
        if (dbFilePath == null || dbFilePath.isBlank()) {
            return null;
        }

        String actualFileName = dbFilePath.substring(dbFilePath.lastIndexOf("/") + 1);
        String safeBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        return String.format("%s/api/v1/resources/audio/scenarios/%d/%s",
                safeBaseUrl, battleId, actualFileName);
    }
}
