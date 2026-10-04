package com.swyp.picke.domain.battle.thumbnail;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleThumbnailCandidateRequest;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 배틀 내용 + 공통 스타일로 썸네일 후보를 여러 장 만들어 S3 에 올린다.
 * 배틀 원문은 먼저 장면 묘사로 바꿔 후보마다 다른 장면을 쓰고, 후보는 동시에 생성한다.
 * 일부가 실패해도 성공한 것만 돌려준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleThumbnailCandidateService {

    static final int CANDIDATE_COUNT = 3;

    private final BattleThumbnailStyleSource styleSource;
    private final ThumbnailSceneWriter sceneWriter;
    private final OpenAiImageClient imageClient;
    private final S3UploadService s3UploadService;
    private final S3PresignedUrlService s3PresignedUrlService;

    public AdminBattleThumbnailCandidatesResponse generate(AdminBattleThumbnailCandidateRequest request) {
        String template = styleSource.promptTemplate();
        List<ReferenceImage> references = styleSource.referenceImages();
        List<String> prompts = scenesFor(request).stream()
                .map(scene -> buildPrompt(template, scene))
                .toList();

        AtomicInteger blockedCount = new AtomicInteger();
        List<Candidate> candidates;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Candidate>> futures = prompts.stream()
                    .map(prompt -> CompletableFuture.supplyAsync(
                            () -> generateOne(prompt, references, blockedCount), executor))
                    .toList();
            candidates = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        }

        if (candidates.isEmpty()) {
            throw new CustomException(blockedCount.get() > 0
                    ? ErrorCode.BATTLE_THUMBNAIL_MODERATION_BLOCKED
                    : ErrorCode.BATTLE_THUMBNAIL_GENERATION_FAILED);
        }
        return new AdminBattleThumbnailCandidatesResponse(candidates);
    }

    /** 후보 수만큼 장면을 만든다. 장면 묘사에 실패하면 배틀 원문을 그대로 쓴다. */
    private List<String> scenesFor(AdminBattleThumbnailCandidateRequest request) {
        List<String> scenes = sceneWriter.write(request, CANDIDATE_COUNT);
        if (scenes.isEmpty()) {
            scenes = List.of(ThumbnailSceneWriter.describeBattle(request));
        }
        List<String> source = scenes;
        return IntStream.range(0, CANDIDATE_COUNT)
                .mapToObj(i -> source.get(i % source.size()))
                .toList();
    }

    private Candidate generateOne(String prompt, List<ReferenceImage> references, AtomicInteger blockedCount) {
        try {
            byte[] image = imageClient.generate(prompt, references);
            String key = upload(image);
            return new Candidate(key, s3PresignedUrlService.generatePresignedUrl(key));
        } catch (ImageModerationBlockedException e) {
            blockedCount.incrementAndGet();
            log.warn("[Thumbnail] 썸네일 후보 1장이 안전 필터에 막힘. prompt={}", prompt, e);
            return null;
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

    static String buildPrompt(String template, String scene) {
        return template.replace("{scene}", scene);
    }
}
