package com.swyp.picke.domain.battle.thumbnail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleThumbnailCandidateRequest;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleThumbnailCandidateRequest.Side;
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

    private static final String TEMPLATE = "T={title} S={summary} D={description} A={optionA} B={optionB}";

    @Mock
    private BattleThumbnailStyleSource styleSource;
    @Mock
    private OpenAiImageClient imageClient;
    @Mock
    private S3UploadService s3UploadService;
    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    private BattleThumbnailCandidateService service;

    private final AdminBattleThumbnailCandidateRequest request = new AdminBattleThumbnailCandidateRequest(
            "트롤리 딜레마", "다섯을 살릴까", null,
            new Side("레버를 당긴다", "다수를 구해야 한다"),
            new Side("가만히 있는다", null));

    @BeforeEach
    void setUp() {
        service = new BattleThumbnailCandidateService(styleSource, imageClient, s3UploadService, s3PresignedUrlService);
        lenient().when(styleSource.promptTemplate()).thenReturn(TEMPLATE);
        lenient().when(styleSource.referenceImages()).thenReturn(List.of());
    }

    @Test
    void 배틀_내용으로_프롬프트_자리표시자를_채운다() {
        String prompt = BattleThumbnailCandidateService.buildPrompt(TEMPLATE, request);

        assertThat(prompt).isEqualTo(
                "T=트롤리 딜레마 S=다섯을 살릴까 D= A=레버를 당긴다 - 다수를 구해야 한다 B=가만히 있는다");
    }

    @Test
    void 후보를_3장_생성해_S3_key와_미리보기_URL을_돌려준다() {
        AtomicInteger seq = new AtomicInteger();
        when(imageClient.generate(anyString(), anyList())).thenReturn(new byte[]{1, 2, 3});
        when(s3UploadService.uploadFile(anyString(), any(File.class)))
                .thenAnswer(inv -> "images/battles/ai-" + seq.incrementAndGet() + ".png");
        when(s3PresignedUrlService.generatePresignedUrl(anyString()))
                .thenAnswer(inv -> "https://signed/" + inv.getArgument(0));

        AdminBattleThumbnailCandidatesResponse response = service.generate(request);

        assertThat(response.candidates()).hasSize(BattleThumbnailCandidateService.CANDIDATE_COUNT);
        assertThat(response.candidates())
                .allSatisfy(c -> {
                    assertThat(c.key()).startsWith("images/battles/ai-");
                    assertThat(c.previewUrl()).isEqualTo("https://signed/" + c.key());
                });
        verify(imageClient, times(BattleThumbnailCandidateService.CANDIDATE_COUNT))
                .generate(eq(BattleThumbnailCandidateService.buildPrompt(TEMPLATE, request)), anyList());
    }

    @Test
    void 일부_실패하면_성공한_후보만_돌려준다() {
        AtomicInteger calls = new AtomicInteger();
        when(imageClient.generate(anyString(), anyList())).thenAnswer(inv -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("boom");
            }
            return new byte[]{1};
        });
        when(s3UploadService.uploadFile(anyString(), any(File.class))).thenAnswer(inv -> inv.getArgument(0));
        when(s3PresignedUrlService.generatePresignedUrl(anyString())).thenReturn("https://signed");

        AdminBattleThumbnailCandidatesResponse response = service.generate(request);

        assertThat(response.candidates()).hasSize(BattleThumbnailCandidateService.CANDIDATE_COUNT - 1);
    }

    @Test
    void 전부_실패하면_예외를_던진다() {
        when(imageClient.generate(anyString(), anyList())).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BATTLE_THUMBNAIL_GENERATION_FAILED);
    }
}
