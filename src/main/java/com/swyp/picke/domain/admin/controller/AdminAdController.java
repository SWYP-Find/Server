package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.admin.dto.ad.request.AdCreativeRequest;
import com.swyp.picke.domain.admin.dto.ad.request.AdStatusRequest;
import com.swyp.picke.domain.admin.dto.ad.response.AdClickLogResponse;
import com.swyp.picke.domain.admin.dto.ad.response.AdCreativeResponse;
import com.swyp.picke.domain.admin.dto.ad.response.AdStatsResponse;
import com.swyp.picke.domain.admin.service.AdminAdService;
import com.swyp.picke.global.common.response.ApiResponse;
import com.swyp.picke.global.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 제휴 광고 API", description = "제휴 광고 소재 관리 및 노출/클릭 집계")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/ads")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAdController {

    private final AdminAdService adminAdService;

    @Operation(summary = "광고 소재 목록", description = "매체/지면/상태로 필터링한다. 값을 비우면 전체를 준다.")
    @GetMapping
    public ApiResponse<List<AdCreativeResponse>> findAll(
            @RequestParam(required = false) AdNetwork network,
            @RequestParam(required = false) AdSlotCode slot,
            @RequestParam(required = false) AdStatus status
    ) {
        return ApiResponse.onSuccess(adminAdService.findAll(network, slot, status));
    }

    @Operation(summary = "광고 소재 등록", description = "각 매체 콘솔에서 발급한 제휴 링크를 그대로 넣는다.")
    @PostMapping
    public ApiResponse<AdCreativeResponse> create(@Valid @RequestBody AdCreativeRequest request) {
        return ApiResponse.onSuccess(adminAdService.create(request));
    }

    @Operation(summary = "광고 소재 수정", description = "매체 API가 동기화하는 소재는 수정할 수 없다.")
    @PutMapping("/{creativeId}")
    public ApiResponse<AdCreativeResponse> update(
            @Parameter(description = "소재 ID", example = "1")
            @PathVariable Long creativeId,
            @Valid @RequestBody AdCreativeRequest request
    ) {
        return ApiResponse.onSuccess(adminAdService.update(creativeId, request));
    }

    @Operation(summary = "광고 소재 게재 상태 변경",
            description = "동기화 소재도 끌 수 있다. PAUSED 는 동기화가 되돌리지 않는다.")
    @PatchMapping("/{creativeId}/status")
    public ApiResponse<AdCreativeResponse> changeStatus(
            @Parameter(description = "소재 ID", example = "1")
            @PathVariable Long creativeId,
            @Valid @RequestBody AdStatusRequest request
    ) {
        return ApiResponse.onSuccess(adminAdService.changeStatus(creativeId, request.status()));
    }

    @Operation(summary = "광고 소재 삭제", description = "매체 API가 동기화하는 소재는 삭제할 수 없다.")
    @DeleteMapping("/{creativeId}")
    public ApiResponse<Void> delete(
            @Parameter(description = "소재 ID", example = "1")
            @PathVariable Long creativeId
    ) {
        adminAdService.delete(creativeId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "소재별 노출/클릭/CTR",
            description = "우리 DB 기준 수치다. 제휴사 정산 리포트와 대조하는 용도로 쓴다.")
    @GetMapping("/stats")
    public ApiResponse<List<AdStatsResponse>> findStats(
            @Parameter(description = "집계 시작일", example = "2026-09-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "집계 종료일(포함)", example = "2026-09-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.onSuccess(adminAdService.findStats(from, to));
    }

    @Operation(summary = "광고 클릭 내역", description = "언제 어떤 소재가 눌렸는지 최신순으로 본다.")
    @GetMapping("/clicks")
    public ApiResponse<PageResponse<AdClickLogResponse>> findClickLogs(
            @Parameter(description = "조회 시작일", example = "2026-09-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일(포함)", example = "2026-09-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "1부터 시작", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(adminAdService.findClickLogs(from, to, page, size));
    }
}
