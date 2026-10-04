package com.swyp.picke.domain.battle.thumbnail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * resources/battle-thumbnail/ 아래 파일로 스타일을 읽는다.
 * - prompt.txt : 프롬프트 템플릿
 * - references/ : 예시 썸네일(png/jpg/webp). 파일명 순서대로 image 1, image 2 ... 로 전달된다.
 */
@Slf4j
@Component
public class ClasspathBattleThumbnailStyleSource implements BattleThumbnailStyleSource {

    private static final String PROMPT_PATH = "battle-thumbnail/prompt.txt";
    private static final String REFERENCES_PATTERN = "classpath*:battle-thumbnail/references/*";
    // OpenAI edits 는 레퍼런스 이미지를 최대 16장까지 받는다.
    private static final int MAX_REFERENCES = 16;

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public String promptTemplate() {
        try (InputStream in = new ClassPathResource(PROMPT_PATH).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("썸네일 프롬프트 파일을 읽을 수 없습니다: " + PROMPT_PATH, e);
        }
    }

    @Override
    public List<ReferenceImage> referenceImages() {
        try {
            Resource[] resources = resolver.getResources(REFERENCES_PATTERN);
            List<ReferenceImage> images = new ArrayList<>();
            Arrays.stream(resources)
                    .filter(r -> r.getFilename() != null && mediaTypeOf(r.getFilename()) != null)
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .limit(MAX_REFERENCES)
                    .forEach(r -> images.add(read(r)));
            return images;
        } catch (IOException e) {
            log.warn("[Thumbnail] 예시 이미지 목록 조회 실패 - 레퍼런스 없이 생성", e);
            return List.of();
        }
    }

    private ReferenceImage read(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            String fileName = resource.getFilename();
            return new ReferenceImage(fileName, in.readAllBytes(), mediaTypeOf(fileName));
        } catch (IOException e) {
            throw new IllegalStateException("예시 이미지를 읽을 수 없습니다: " + resource.getFilename(), e);
        }
    }

    private static MediaType mediaTypeOf(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return null;
    }
}
