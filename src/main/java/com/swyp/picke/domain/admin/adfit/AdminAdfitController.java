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
@Tag(name = "관리자 AdFit", description = "AdFit 계정 자동 수익 및 수동 입력 비용·ROI")
public class AdminAdfitController {
    private final AdfitReportService service;

    @GetMapping
    @Operation(summary = "AdFit 기간별 수익·ROI", description = "account는 AdFit 계정 자동 수익, units/days는 수동 입력 비용·ROI. 미입력·계산 불가는 null")
    public ApiResponse<AdfitReport> report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.onSuccess(service.report(from, to));
    }

    @GetMapping("/session-cookie")
    @Operation(summary = "AdFit 세션 쿠키 상태", description = "쿠키 보유 여부와 마지막 갱신 시각만 준다. 쿠키 값은 응답에 담지 않는다")
    public ApiResponse<AdfitSessionCookieStatus> sessionCookieStatus() {
        return ApiResponse.onSuccess(service.sessionCookieStatus());
    }

    @PutMapping("/session-cookie")
    @Operation(summary = "AdFit 세션 쿠키 갱신",
               description = "AdFit 콘솔에 로그인한 브라우저의 Cookie 헤더 전체를 넣는다. 재배포 없이 자동 수익 조회가 다시 붙는다")
    public ApiResponse<Void> updateSessionCookie(@Valid @RequestBody AdfitSessionCookieRequest request) {
        service.updateSessionCookie(request);
        return ApiResponse.onSuccess(null);
    }

    @PutMapping("/daily")
    @Operation(summary = "광고 단위별 일일 수익·배분 비용 저장", description = "같은 날짜·광고 단위는 수정한다. 비용을 여러 단위에 중복 입력하지 않는다.")
    public ApiResponse<Void> save(@Valid @RequestBody AdfitDailyRequest request) {
        service.save(request);
        return ApiResponse.onSuccess(null);
    }
}
