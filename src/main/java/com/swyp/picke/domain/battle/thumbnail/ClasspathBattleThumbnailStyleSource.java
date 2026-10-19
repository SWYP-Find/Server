package com.swyp.picke.domain.battle.thumbnail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * resources/battle-thumbnail/ 아래 파일로 스타일을 읽는다.
 * - prompt.txt : 프롬프트 템플릿
 * - palette.txt : 색상 팔레트(헥사 코드)
 * - references/ : 스타일 예시 썸네일(png/jpg/webp). 파일명 순서대로 전달된다.
 *   같은 이름의 txt(reference1.png ↔ reference1.txt)가 있으면 그 예시의 주제·장면 설명으로 읽는다.
 */
@Slf4j
@Component
public class ClasspathBattleThumbnailStyleSource implements BattleThumbnailStyleSource {

    private static final String PROMPT_PATH = "battle-thumbnail/prompt.txt";
    private static final String PALETTE_PATH = "battle-thumbnail/palette.txt";
    private static final String REFERENCES_PATTERN = "classpath*:battle-thumbnail/references/*";
    // OpenAI edits 는 레퍼런스 이미지를 최대 16장까지 받는다.
    private static final int MAX_REFERENCES = 16;

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public String promptTemplate() {
        return readText(PROMPT_PATH);
    }

    @Override
    public String palette() {
        return readText(PALETTE_PATH).strip();
    }

    private String readText(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("썸네일 스타일 파일을 읽을 수 없습니다: " + path, e);
        }
    }

    @Override
    public List<ReferenceImage> referenceImages() {
        try {
            Resource[] resources = resolver.getResources(REFERENCES_PATTERN);
            Map<String, Resource> captions = Arrays.stream(resources)
                    .filter(r -> r.getFilename() != null && r.getFilename().toLowerCase(Locale.ROOT).endsWith(".txt"))
                    .collect(Collectors.toMap(r -> baseName(r.getFilename()), r -> r, (first, second) -> first));
            List<ReferenceImage> images = new ArrayList<>();
            Arrays.stream(resources)
                    .filter(r -> r.getFilename() != null && mediaTypeOf(r.getFilename()) != null)
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .limit(MAX_REFERENCES)
                    .forEach(r -> images.add(read(r, captions.get(baseName(r.getFilename())))));
            return images;
        } catch (IOException e) {
            log.warn("[Thumbnail] 예시 이미지 목록 조회 실패 - 레퍼런스 없이 생성", e);
            return List.of();
        }
    }

    private ReferenceImage read(Resource resource, Resource captionResource) {
        try (InputStream in = resource.getInputStream()) {
            String fileName = resource.getFilename();
            return new ReferenceImage(fileName, in.readAllBytes(), mediaTypeOf(fileName), readCaption(captionResource));
        } catch (IOException e) {
            throw new IllegalStateException("예시 이미지를 읽을 수 없습니다: " + resource.getFilename(), e);
        }
    }

    private ReferenceCaption readCaption(Resource resource) throws IOException {
        if (resource == null) {
            return ReferenceCaption.NONE;
        }
        try (InputStream in = resource.getInputStream()) {
            return ReferenceCaption.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private static MediaType mediaTypeOf(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return null;
    }
}
