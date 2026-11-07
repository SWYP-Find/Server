package com.swyp.picke.domain.battle.thumbnail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClasspathBattleThumbnailStyleSourceTest {

    private final ClasspathBattleThumbnailStyleSource source = new ClasspathBattleThumbnailStyleSource();

    @Test
    void 프롬프트_템플릿에_자리표시자가_있다() {
        assertThat(source.promptTemplate())
                .contains("{palette}", "{references}", "{scene}");
    }

    @Test
    void 팔레트를_헥사_코드로_읽는다() {
        assertThat(source.palette()).containsPattern("#[0-9A-Fa-f]{6}");
    }

    @Test
    void 예시_이미지를_파일명_순서대로_읽는다() {
        List<ReferenceImage> images = source.referenceImages();

        assertThat(images).isNotEmpty();
        assertThat(images).extracting(ReferenceImage::fileName).isSorted();
        assertThat(images).allSatisfy(image -> {
            assertThat(image.bytes()).isNotEmpty();
            assertThat(image.mediaType().getType()).isEqualTo("image");
        });
    }

    @Test
    void 예시_설명_파일에서_주제와_장면을_읽는다() {
        ReferenceCaption caption = ReferenceCaption.parse("주제: AI가 그린 그림도 예술인가\n장면:  이젤 앞의 로봇 \n");

        assertThat(caption.topic()).isEqualTo("AI가 그린 그림도 예술인가");
        assertThat(caption.scene()).isEqualTo("이젤 앞의 로봇");
        assertThat(caption.hasScene()).isTrue();
        assertThat(ReferenceCaption.parse("장면: 주제 없는 장면").hasScene()).isFalse();
    }
}
