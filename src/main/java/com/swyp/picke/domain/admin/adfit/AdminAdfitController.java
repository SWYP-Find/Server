package com.swyp.picke.domain.admin.adfit;

import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/adfit")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자 AdFit", description = "iOS 광고 단위 및 콘솔 수동 입력 수익·비용·ROI")
public class AdminAdfitController {
    private final AdfitReportService service;

    @GetMapping
    @Operation(summary = "AdFit 광고 단위와 기간별 수익·ROI", description = "미입력 금액 및 계산 불가 ROI는 null")
    public ApiResponse<AdfitReport> report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.onSuccess(service.report(from, to));
    }

    @PutMapping("/daily")
    @Operation(summary = "광고 단위별 일일 수익·배분 비용 저장", description = "같은 날짜·광고 단위는 수정한다. 비용을 여러 단위에 중복 입력하지 않는다.")
    public ApiResponse<Void> save(@Valid @RequestBody AdfitDailyRequest request) {
        service.save(request);
        return ApiResponse.onSuccess(null);
    }
}
