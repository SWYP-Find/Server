package com.swyp.picke.domain.battle.thumbnail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ClasspathBattleThumbnailStyleSourceTest {

    private final ClasspathBattleThumbnailStyleSource source = new ClasspathBattleThumbnailStyleSource();

    @Test
    void 프롬프트_템플릿에_장면_자리표시자가_있다() {
        assertThat(source.promptTemplate())
                .contains("{scene}");
    }

    @Test
    void 예시_이미지와_팔레트를_파일명_순서대로_읽는다() {
        List<ReferenceImage> images = source.referenceImages();

        assertThat(images).extracting(ReferenceImage::fileName)
                .containsExactly("image1.png", "image2.png", "image3.png", "palette.jpg");
        assertThat(images).extracting(ReferenceImage::mediaType)
                .containsExactly(MediaType.IMAGE_PNG, MediaType.IMAGE_PNG, MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG);
        assertThat(images).allSatisfy(image -> assertThat(image.bytes()).isNotEmpty());
    }
}
