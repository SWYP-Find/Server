package com.swyp.picke.domain.battle.thumbnail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleThumbnailCandidateRequest;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleThumbnailCandidatesResponse;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import com.swyp.picke.global.infra.s3.service.S3UploadService;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
class BattleThumbnailCandidateServiceTest {

    private static final String TEMPLATE = "STYLE / {scene}";

    @Mock
    private BattleThumbnailStyleSource styleSource;
    @Mock
    private ThumbnailSceneWriter sceneWriter;
    @Mock
    private OpenAiImageClient imageClient;
    @Mock
    private S3UploadService s3UploadService;
    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    private BattleThumbnailCandidateService service;

    private final AdminBattleThumbnailCandidateRequest request =
            new AdminBattleThumbnailCandidateRequest("트롤리 딜레마", "다섯을 살릴까", null);

    @BeforeEach
    void setUp() {
        service = new BattleThumbnailCandidateService(
                styleSource, sceneWriter, imageClient, s3UploadService, s3PresignedUrlService);
        lenient().when(styleSource.promptTemplate()).thenReturn(TEMPLATE);
        lenient().when(styleSource.palette()).thenReturn("PALETTE");
        lenient().when(styleSource.referenceImages()).thenReturn(List.of());
        lenient().when(sceneWriter.write(any(), anyInt(), anyList())).thenReturn(List.of("장면1", "장면2", "장면3"));
    }

    private void stubUploadSuccess() {
        AtomicInteger seq = new AtomicInteger();
        lenient().when(s3UploadService.uploadFile(anyString(), any(File.class)))
                .thenAnswer(inv -> "images/battles/ai-" + seq.incrementAndGet() + ".png");
        lenient().when(s3PresignedUrlService.generatePresignedUrl(anyString()))
                .thenAnswer(inv -> "https://signed/" + inv.getArgument(0));
    }

    @Test
    void 후보마다_다른_장면으로_3장을_생성해_S3_key와_미리보기_URL을_돌려준다() {
        when(imageClient.generate(anyString(), anyList())).thenReturn(new byte[]{1, 2, 3});
        stubUploadSuccess();

        AdminBattleThumbnailCandidatesResponse response = service.generate(request);

        assertThat(response.candidates()).hasSize(BattleThumbnailCandidateService.CANDIDATE_COUNT);
        assertThat(response.candidates()).allSatisfy(c -> {
            assertThat(c.key()).startsWith("images/battles/ai-");
            assertThat(c.previewUrl()).isEqualTo("https://signed/" + c.key());
        });
        verify(imageClient).generate(eq("STYLE / 장면1"), anyList());
        verify(imageClient).generate(eq("STYLE / 장면2"), anyList());
        verify(imageClient).generate(eq("STYLE / 장면3"), anyList());
    }

    @Test
    void 장면_묘사에_실패하면_제목_기반_원문으로_생성한다() {
        when(sceneWriter.write(any(), anyInt(), anyList())).thenReturn(List.of());
        when(imageClient.generate(anyString(), anyList())).thenReturn(new byte[]{1});
        stubUploadSuccess();

        service.generate(request);

        String fallback = "STYLE / " + ThumbnailSceneWriter.describeBattle(request);
        verify(imageClient, times(BattleThumbnailCandidateService.CANDIDATE_COUNT)).generate(eq(fallback), anyList());
        assertThat(fallback).contains("트롤리 딜레마").doesNotContain("A 입장", "B 입장");
    }

    @Test
    void 프롬프트에_팔레트와_예시_이미지_장수를_채운다() {
        String template = "{palette} / {references} / {scene}";
        List<ReferenceImage> references = List.of(reference(ReferenceCaption.NONE), reference(ReferenceCaption.NONE));

        String withReferences = BattleThumbnailCandidateService.buildPrompt(template, "#492C24", references, "장면");
        String withoutReferences = BattleThumbnailCandidateService.buildPrompt(template, "#492C24", List.of(), "장면");

        assertThat(withReferences).startsWith("#492C24 / ").endsWith(" / 장면").contains("이미지 2장");
        assertThat(withReferences).doesNotContain("번째 예시");
        assertThat(withoutReferences).contains("예시 이미지는 없습니다").doesNotContain("0장");
        assertThat(withReferences + withoutReferences).doesNotContain("{");
    }

    @Test
    void 설명이_있는_예시는_첨부_순서_번호와_함께_주제와_장면을_알려준다() {
        List<ReferenceImage> references = List.of(
                reference(ReferenceCaption.NONE),
                reference(new ReferenceCaption("AI 예술", "이젤 앞의 로봇")),
                reference(new ReferenceCaption("전쟁", "")));

        String prompt = BattleThumbnailCandidateService.buildPrompt("{references}", "", references, "");

        assertThat(prompt)
                .contains("2번째 예시: 주제 \"AI 예술\" → 장면 \"이젤 앞의 로봇\"")
                .contains("3번째 예시: 주제 \"전쟁\"")
                .doesNotContain("1번째 예시", "\"전쟁\" →");
    }

    @Test
    void 장면_기획에는_주제와_장면이_모두_있는_예시만_넘긴다() {
        List<ReferenceImage> references = List.of(
                reference(new ReferenceCaption("AI 예술", "이젤 앞의 로봇")),
                reference(new ReferenceCaption("전쟁", "")));

        assertThat(ThumbnailSceneWriter.examples(references))
                .contains("- 제목: AI 예술 → 장면: 이젤 앞의 로봇")
                .doesNotContain("전쟁");
        assertThat(ThumbnailSceneWriter.examples(List.of(reference(ReferenceCaption.NONE)))).isEmpty();
    }

    private static ReferenceImage reference(ReferenceCaption caption) {
        return new ReferenceImage("image.png", new byte[]{1}, MediaType.IMAGE_PNG, caption);
    }

    @Test
    void 일부_실패하면_성공한_후보만_돌려준다() {
        when(imageClient.generate(eq("STYLE / 장면1"), anyList())).thenThrow(new IllegalStateException("boom"));
        when(imageClient.generate(eq("STYLE / 장면2"), anyList())).thenReturn(new byte[]{1});
        when(imageClient.generate(eq("STYLE / 장면3"), anyList())).thenReturn(new byte[]{1});
        stubUploadSuccess();

        AdminBattleThumbnailCandidatesResponse response = service.generate(request);

        assertThat(response.candidates()).hasSize(BattleThumbnailCandidateService.CANDIDATE_COUNT - 1);
    }

    @Test
    void 전부_안전_필터에_막히면_안전_정책_에러를_던진다() {
        when(imageClient.generate(anyString(), anyList()))
                .thenThrow(new ImageModerationBlockedException("blocked", null));

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BATTLE_THUMBNAIL_MODERATION_BLOCKED);
    }

    @Test
    void 전부_일반_오류로_실패하면_생성_실패_에러를_던진다() {
        when(imageClient.generate(anyString(), anyList())).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BATTLE_THUMBNAIL_GENERATION_FAILED);
    }
}
