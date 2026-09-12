package com.swyp.picke.domain.scenario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.philosophervoice.request.PhilosopherVoiceRequest;
import com.swyp.picke.domain.admin.dto.philosophervoice.response.PhilosopherVoiceResponse;
import com.swyp.picke.domain.scenario.entity.PhilosopherVoice;
import com.swyp.picke.domain.scenario.repository.PhilosopherVoiceRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhilosopherVoiceServiceTest {

    @Mock
    private PhilosopherVoiceRepository philosopherVoiceRepository;

    @Mock
    private ResourceUrlProvider resourceUrlProvider;

    @InjectMocks
    private PhilosopherVoiceService philosopherVoiceService;

    @BeforeEach
    void setUp() {
        // raw key를 그대로 돌려주는 기본 스텁(변환 여부 자체를 검증하는 테스트만 다르게 재정의)
        lenient().when(resourceUrlProvider.getImageUrl(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    private PhilosopherVoice entity(String name, String referenceId) {
        return PhilosopherVoice.builder()
                .name(name)
                .referenceId(referenceId)
                .voiceLabel("label")
                .build();
    }

    @Test
    void findReferenceIdByName_등록된_철학자면_reference_id_반환() {
        when(philosopherVoiceRepository.findByName("소크라테스"))
                .thenReturn(Optional.of(entity("소크라테스", "voice-a")));

        assertThat(philosopherVoiceService.findReferenceIdByName(" 소크라테스 "))
                .contains("voice-a");
    }

    @Test
    void findReferenceIdByName_미등록이거나_빈값이면_empty() {
        when(philosopherVoiceRepository.findByName("루소")).thenReturn(Optional.empty());

        assertThat(philosopherVoiceService.findReferenceIdByName("루소")).isEmpty();
        assertThat(philosopherVoiceService.findReferenceIdByName(null)).isEmpty();
        assertThat(philosopherVoiceService.findReferenceIdByName("  ")).isEmpty();
    }

    @Test
    void create_이름이_중복이면_예외() {
        when(philosopherVoiceRepository.existsByName("칸트")).thenReturn(true);

        assertThatThrownBy(() -> philosopherVoiceService.create(
                new PhilosopherVoiceRequest("칸트", "voice-x", null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHILOSOPHER_VOICE_DUPLICATED);

        verify(philosopherVoiceRepository, never()).save(any());
    }

    @Test
    void create_정상이면_저장하고_응답_반환() {
        when(philosopherVoiceRepository.existsByName("칸트")).thenReturn(false);
        when(philosopherVoiceRepository.save(any(PhilosopherVoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PhilosopherVoiceResponse response = philosopherVoiceService.create(
                new PhilosopherVoiceRequest(" 칸트 ", " voice-x ", "Meursault", null, null));

        assertThat(response.name()).isEqualTo("칸트");
        assertThat(response.referenceId()).isEqualTo("voice-x");
        verify(philosopherVoiceRepository).save(any(PhilosopherVoice.class));
    }

    @Test
    void update_대상이_없으면_예외() {
        when(philosopherVoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> philosopherVoiceService.update(99L,
                new PhilosopherVoiceRequest("칸트", "voice-x", null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHILOSOPHER_VOICE_NOT_FOUND);
    }

    @Test
    void update_reference_id를_교체한다() {
        PhilosopherVoice found = entity("칸트", "voice-old");
        when(philosopherVoiceRepository.findById(1L)).thenReturn(Optional.of(found));

        PhilosopherVoiceResponse response = philosopherVoiceService.update(1L,
                new PhilosopherVoiceRequest("칸트", "voice-new", "새 라벨", null, "메모"));

        assertThat(response.referenceId()).isEqualTo("voice-new");
        assertThat(found.getReferenceId()).isEqualTo("voice-new");
    }

    @Test
    void 응답의_imageKey는_raw_저장키가_아니라_ResourceUrlProvider가_변환한_값이다() {
        when(philosopherVoiceRepository.existsByName("루소")).thenReturn(false);
        when(philosopherVoiceRepository.save(any(PhilosopherVoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceUrlProvider.getImageUrl(FileCategory.PHILOSOPHER, "images/philosophers/rousseau.png"))
                .thenReturn("https://dev.picke.store/api/v1/resources/images/PHILOSOPHER/rousseau.png");

        PhilosopherVoiceResponse response = philosopherVoiceService.create(
                new PhilosopherVoiceRequest("루소", "voice-x", null, "images/philosophers/rousseau.png", null));

        assertThat(response.imageKey())
                .isEqualTo("https://dev.picke.store/api/v1/resources/images/PHILOSOPHER/rousseau.png")
                .isNotEqualTo("images/philosophers/rousseau.png");
    }

    @Test
    void create_및_update가_imageKey를_저장한다() {
        when(philosopherVoiceRepository.existsByName("루소")).thenReturn(false);
        when(philosopherVoiceRepository.save(any(PhilosopherVoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PhilosopherVoiceResponse created = philosopherVoiceService.create(
                new PhilosopherVoiceRequest("루소", "voice-x", "미호크 장정진",
                        "images/philosophers/rousseau.png", null));
        assertThat(created.imageKey()).isEqualTo("images/philosophers/rousseau.png");

        PhilosopherVoice found = entity("루소", "voice-x");
        when(philosopherVoiceRepository.findById(2L)).thenReturn(Optional.of(found));

        PhilosopherVoiceResponse updated = philosopherVoiceService.update(2L,
                new PhilosopherVoiceRequest("루소", "voice-x", null,
                        "images/philosophers/rousseau-v2.png", null));
        assertThat(updated.imageKey()).isEqualTo("images/philosophers/rousseau-v2.png");
    }

    @Test
    void delete_대상이_없으면_예외() {
        when(philosopherVoiceRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> philosopherVoiceService.delete(99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHILOSOPHER_VOICE_NOT_FOUND);

        verify(philosopherVoiceRepository, never()).deleteById(any());
    }
}
