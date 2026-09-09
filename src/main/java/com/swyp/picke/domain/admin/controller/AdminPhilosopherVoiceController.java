package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.philosophervoice.request.PhilosopherVoiceRequest;
import com.swyp.picke.domain.admin.dto.philosophervoice.response.PhilosopherVoiceResponse;
import com.swyp.picke.domain.scenario.service.PhilosopherVoiceService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 철학자 보이스 API", description = "철학자 이름 → Fish Audio 보이스(reference_id) 매핑 관리")
@RestController
@RequestMapping("/api/v1/admin/philosopher-voices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPhilosopherVoiceController {

    private final PhilosopherVoiceService philosopherVoiceService;

    @Operation(summary = "철학자 보이스 매핑 목록")
    @GetMapping
    public ApiResponse<List<PhilosopherVoiceResponse>> getAll() {
        return ApiResponse.onSuccess(philosopherVoiceService.getAll());
    }

    @Operation(summary = "철학자 보이스 매핑 생성")
    @PostMapping
    public ApiResponse<PhilosopherVoiceResponse> create(@Valid @RequestBody PhilosopherVoiceRequest request) {
        return ApiResponse.onSuccess(philosopherVoiceService.create(request));
    }

    @Operation(summary = "철학자 보이스 매핑 수정")
    @PatchMapping("/{id}")
    public ApiResponse<PhilosopherVoiceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PhilosopherVoiceRequest request
    ) {
        return ApiResponse.onSuccess(philosopherVoiceService.update(id, request));
    }

    @Operation(summary = "철학자 보이스 매핑 삭제")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        philosopherVoiceService.delete(id);
        return ApiResponse.onSuccess(null);
    }
}
