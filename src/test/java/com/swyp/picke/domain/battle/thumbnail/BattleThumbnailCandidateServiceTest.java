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
        lenient().when(styleSource.referenceImages()).thenReturn(List.of());
        lenient().when(sceneWriter.write(any(), anyInt())).thenReturn(List.of("장면1", "장면2", "장면3"));
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
        when(sceneWriter.write(any(), anyInt())).thenReturn(List.of());
        when(imageClient.generate(anyString(), anyList())).thenReturn(new byte[]{1});
        stubUploadSuccess();

        service.generate(request);

        String fallback = "STYLE / " + ThumbnailSceneWriter.describeBattle(request);
        verify(imageClient, times(BattleThumbnailCandidateService.CANDIDATE_COUNT)).generate(eq(fallback), anyList());
        assertThat(fallback).contains("트롤리 딜레마").doesNotContain("A 입장", "B 입장");
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
