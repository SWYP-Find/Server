package com.swyp.picke.global.infra.s3.util;

import com.swyp.picke.global.infra.s3.enums.FileCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResourceUrlProvider {

    @Value("${picke.baseUrl}")
    private String baseUrl;

    /**
     * 이미지 리소스용 URL 생성 (컨트롤러의 /images/{category}/{fileName}와 매칭)
     */
    public String getImageUrl(FileCategory category, String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }
        // S3 Key 전체 경로에서 파일명만 추출
        String fileName = s3Key.substring(s3Key.lastIndexOf("/") + 1);

        // 결과: {baseUrl}/api/v1/resources/images/BATTLE/uuid_name.png
        return String.format("%s/api/v1/resources/images/%s/%s",
                baseUrl, category.name(), fileName);
    }

    /**
     * 시나리오 오디오용 URL 생성 (컨트롤러의 /audio/scenarios/{battleId}/{fileName}와 매칭)
     */
    public String getAudioUrl(Long battleId, String dbFilePath) {
        if (dbFilePath == null || dbFilePath.isBlank()) {
            return null;
        }

        // 1. DB에 저장된 "/audio/scenarios/17/COMMON.mp3"에서 "COMMON.mp3"만 추출!
        String actualFileName = dbFilePath.substring(dbFilePath.lastIndexOf("/") + 1);

        // 2. baseUrl 끝에 '/'가 있다면 제거하여 '//api/v1...' 중복 방지
        String safeBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        // 3. 최종 URL 깔끔하게 조립
        return String.format("%s/api/v1/resources/audio/scenarios/%d/%s",
                safeBaseUrl, battleId, actualFileName);
    }
}