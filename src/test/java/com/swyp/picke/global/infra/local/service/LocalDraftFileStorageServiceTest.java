package com.swyp.picke.global.infra.local.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.service.S3UploadService;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class LocalDraftFileStorageServiceTest {

    private LocalDraftFileStorageService service;
    private S3UploadService s3UploadService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        service = new LocalDraftFileStorageService();
        ReflectionTestUtils.setField(service, "localStorageRoot", tempDir.toString());
        ReflectionTestUtils.setField(service, "baseUrl", "https://dev.picke.store");
        s3UploadService = mock(S3UploadService.class);
    }

    private String saveDraft() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "rousseau.png", "image/png", new byte[]{1, 2, 3});
        return service.saveDraftFile(file);
    }

    @Test
    void resolveS3Key는_실제_업로드_없이_목적지_key만_계산한다() throws IOException {
        String localKey = saveDraft();

        String s3Key = service.resolveS3Key(localKey, FileCategory.PHILOSOPHER);

        assertThat(s3Key).startsWith("images/philosophers/").endsWith("rousseau.png");
        // 부수효과 없음: 로컬 파일 그대로 남아있고 S3 호출 없음
        verify(s3UploadService, never()).uploadFile(anyString(), any());
    }

    @Test
    void resolveS3Key는_draft_파일이_없으면_예외를_던진다() {
        assertThatThrownBy(() -> service.resolveS3Key("local/drafts/missing_file.png", FileCategory.PHILOSOPHER))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void promoteToS3는_업로드하고_로컬_draft를_삭제한다() throws IOException {
        String localKey = saveDraft();
        String s3Key = service.resolveS3Key(localKey, FileCategory.PHILOSOPHER);
        when(s3UploadService.uploadFile(eq(s3Key), any()))
                .thenReturn(s3Key);

        service.promoteToS3(localKey, s3Key, s3UploadService);

        verify(s3UploadService).uploadFile(eq(s3Key), any());
        assertThat(service.isLocalDraftReference(localKey)).isTrue(); // 여전히 local draft 형식의 키지만
        // 실제 파일은 삭제됐어야 한다 -> 다시 promote 시도해도 재업로드 없이 조용히 스킵
        service.promoteToS3(localKey, s3Key, s3UploadService);
        verify(s3UploadService, times(1))
                .uploadFile(eq(s3Key), any());
    }

    @Test
    void promoteToS3는_local_draft가_아니면_아무일도_하지_않는다() {
        service.promoteToS3("images/philosophers/already-s3.png", "images/philosophers/already-s3.png", s3UploadService);

        verify(s3UploadService, never()).uploadFile(anyString(), any());
    }
}
