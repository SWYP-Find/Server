package com.swyp.picke.domain.battle.thumbnail;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleThumbnailCandidateRequest;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleThumbnailCandidateRequest.Side;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleThumbnailCandidatesResponse;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleThumbnailCandidatesResponse.Candidate;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import com.swyp.picke.global.infra.s3.service.S3UploadService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 배틀 내용 + 공통 스타일로 썸네일 후보를 여러 장 만들어 S3 에 올린다.
 * 후보는 동시에 생성하고, 일부가 실패해도 성공한 것만 돌려준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleThumbnailCandidateService {

    static final int CANDIDATE_COUNT = 3;

    private final BattleThumbnailStyleSource styleSource;
    private final OpenAiImageClient imageClient;
    private final S3UploadService s3UploadService;
    private final S3PresignedUrlService s3PresignedUrlService;

    public AdminBattleThumbnailCandidatesResponse generate(AdminBattleThumbnailCandidateRequest request) {
        String prompt = buildPrompt(styleSource.promptTemplate(), request);
        List<ReferenceImage> references = styleSource.referenceImages();

        List<Candidate> candidates;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Candidate>> futures = IntStream.range(0, CANDIDATE_COUNT)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> generateOne(prompt, references), executor))
                    .toList();
            candidates = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        }

        if (candidates.isEmpty()) {
            throw new CustomException(ErrorCode.BATTLE_THUMBNAIL_GENERATION_FAILED);
        }
        return new AdminBattleThumbnailCandidatesResponse(candidates);
    }

    private Candidate generateOne(String prompt, List<ReferenceImage> references) {
        try {
            byte[] image = imageClient.generate(prompt, references);
            String key = upload(image);
            return new Candidate(key, s3PresignedUrlService.generatePresignedUrl(key));
        } catch (Exception e) {
            log.warn("[Thumbnail] 썸네일 후보 1장 생성 실패", e);
            return null;
        }
    }

    private String upload(byte[] image) throws IOException {
        File temp = Files.createTempFile("battle-thumbnail-", ".png").toFile();
        try {
            Files.write(temp.toPath(), image);
            String key = FileCategory.BATTLE.getPath() + "/ai-" + UUID.randomUUID() + ".png";
            return s3UploadService.uploadFile(key, temp);
        } finally {
            Files.deleteIfExists(temp.toPath());
        }
    }

    static String buildPrompt(String template, AdminBattleThumbnailCandidateRequest request) {
        return template
                .replace("{title}", orEmpty(request.title()))
                .replace("{summary}", orEmpty(request.summary()))
                .replace("{description}", orEmpty(request.description()))
                .replace("{optionA}", describe(request.optionA()))
                .replace("{optionB}", describe(request.optionB()));
    }

    private static String describe(Side side) {
        if (side == null) {
            return "";
        }
        String title = orEmpty(side.title());
        String stance = orEmpty(side.stance());
        if (stance.isEmpty()) {
            return title;
        }
        return title.isEmpty() ? stance : title + " - " + stance;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
