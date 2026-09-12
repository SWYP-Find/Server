package com.swyp.picke.domain.scenario.service;

import com.swyp.picke.domain.admin.dto.philosophervoice.request.PhilosopherVoiceRequest;
import com.swyp.picke.domain.admin.dto.philosophervoice.response.PhilosopherVoiceResponse;
import com.swyp.picke.domain.scenario.entity.PhilosopherVoice;
import com.swyp.picke.domain.scenario.repository.PhilosopherVoiceRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhilosopherVoiceService {

    private final PhilosopherVoiceRepository philosopherVoiceRepository;
    private final ResourceUrlProvider resourceUrlProvider;

    @Transactional(readOnly = true)
    public List<PhilosopherVoiceResponse> getAll() {
        return philosopherVoiceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /** 파서가 발화자 철학자에 맞는 보이스를 채울 때 사용. 없으면 empty. */
    @Transactional(readOnly = true)
    public Optional<String> findReferenceIdByName(String philosopherName) {
        if (philosopherName == null || philosopherName.isBlank()) {
            return Optional.empty();
        }
        return philosopherVoiceRepository.findByName(philosopherName.trim())
                .map(PhilosopherVoice::getReferenceId);
    }

    @Transactional
    public PhilosopherVoiceResponse create(PhilosopherVoiceRequest request) {
        if (philosopherVoiceRepository.existsByName(request.name().trim())) {
            throw new CustomException(ErrorCode.PHILOSOPHER_VOICE_DUPLICATED);
        }
        PhilosopherVoice saved = philosopherVoiceRepository.save(PhilosopherVoice.builder()
                .name(request.name().trim())
                .referenceId(request.referenceId().trim())
                .voiceLabel(request.voiceLabel())
                .imageKey(request.imageKey())
                .note(request.note())
                .build());
        return toResponse(saved);
    }

    @Transactional
    public PhilosopherVoiceResponse update(Long id, PhilosopherVoiceRequest request) {
        PhilosopherVoice entity = philosopherVoiceRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PHILOSOPHER_VOICE_NOT_FOUND));
        entity.update(request.referenceId(), request.voiceLabel(), request.imageKey(), request.note());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!philosopherVoiceRepository.existsById(id)) {
            throw new CustomException(ErrorCode.PHILOSOPHER_VOICE_NOT_FOUND);
        }
        philosopherVoiceRepository.deleteById(id);
    }

    /**
     * imageKey는 DB에 raw 저장 키로 있다가, 응답 시 다른 이미지 필드(썸네일/옵션 이미지)와
     * 동일하게 호출 가능한 경로로 변환해서 나간다({@link ResourceUrlProvider}).
     */
    private PhilosopherVoiceResponse toResponse(PhilosopherVoice entity) {
        String imageUrl = resourceUrlProvider.getImageUrl(FileCategory.PHILOSOPHER, entity.getImageKey());
        return PhilosopherVoiceResponse.from(entity, imageUrl);
    }
}
