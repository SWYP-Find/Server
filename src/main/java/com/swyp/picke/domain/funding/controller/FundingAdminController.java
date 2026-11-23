package com.swyp.picke.domain.funding.controller;

import com.swyp.picke.domain.admin.dto.funding.request.ManualTumblbugBuyerRequest;
import com.swyp.picke.domain.admin.dto.funding.response.ManualTumblbugBuyerResponse;
import com.swyp.picke.domain.funding.dto.request.*;
import com.swyp.picke.domain.funding.dto.response.*;
import com.swyp.picke.domain.funding.service.FundingAdminService;
import com.swyp.picke.domain.funding.service.FundingTicketQueryService;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.global.common.response.PageResponse;
import java.time.LocalDate;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "텀블벅 엑셀 운영", description = "최신 클래스 이용권에 연결되는 XLSX 가져오기와 디지털 선물 전달")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class FundingAdminController {
    private final FundingAdminService fundingAdminService;
    private final FundingTicketQueryService fundingTicketQueryService;

    @Operation(summary = "펀딩 이용권·등록자 검색", description = "최신 클래스 이용권을 정원, 상태, 등록일, 구매자/등록자, 전체 코드 또는 끝 4자리로 검색합니다. 연락처와 코드는 마스킹됩니다.")
    @GetMapping("/tickets")
    public ApiResponse<PageResponse<TicketAdminRowResponse>> searchTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) LocalDate registeredFrom,
            @RequestParam(required = false) LocalDate registeredTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.onSuccess(fundingTicketQueryService.searchTickets(keyword, capacity, status,
                registeredFrom, registeredTo, page, size));
    }

    @Operation(summary = "펀딩 이용권 상세", description = "구매자·등록자·이용권 상태·전달 이력과 연결된 클래스를 조회합니다. 코드 원문은 별도 조회 API를 사용합니다.")
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<ApiResponse<TicketAdminDetailResponse>> getTicketDetail(@PathVariable Long ticketId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.onSuccess(fundingTicketQueryService.getTicketDetail(ticketId)));
    }

    @Operation(summary = "텀블벅 후원자 엑셀 자동 판별 및 미리보기", description = "선택한 리워드 시트에서 사진의 9개 표 열을 자동 판별합니다. 미리보기는 저장이나 코드 발급을 하지 않습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "엑셀 검토 결과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "엑셀 또는 캠페인 옵션 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "사진의 필수 열을 자동 판별하지 못함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "엑셀 10MB 초과")
    })
    @PostMapping(value = "/funding-orders/tumblbug/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TumblbugImportPreviewResponse> previewTumblbugSupporters(
            @Valid @RequestPart("options") TumblbugSupporterImportRequest options,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.onSuccess(fundingAdminService.previewTumblbugSupporters(options, readXlsx(file)));
    }

    @Operation(summary = "텀블벅 후원자 엑셀 검사", description = "모든 시트의 열 제목을 자동 판별하고 화면에 표시된 후원자 표와 샘플 행을 반환합니다. 데이터는 저장하지 않습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "워크북 시트 및 행 미리보기"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "XLSX 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "엑셀 10MB 초과")
    })
    @PostMapping(value = "/funding-orders/tumblbug/inspect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TumblbugWorkbookInspectResponse> inspectTumblbugWorkbook(
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.onSuccess(fundingAdminService.inspectTumblbugWorkbook(readXlsx(file)));
    }

    @Operation(summary = "텀블벅 구매자 공통 등록", description = "엑셀 명단을 후원번호·리워드 기준으로 신규 등록하거나 업데이트합니다. 이용권 코드는 발급하지 않습니다.")
    @PostMapping(value = "/funding-orders/tumblbug/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TumblbugSupporterImportResponse>> importTumblbugSupporters(
            @Valid @RequestPart("options") TumblbugSupporterImportRequest options,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long adminUserId
    ) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.onSuccess(fundingAdminService.importTumblbugSupporters(options, readXlsx(file), adminUserId)));
    }

    @Operation(summary = "수정된 텀블벅 표 저장", description = "엑셀 미리보기에서 직접 편집한 행을 검증한 뒤 신규 등록하거나 기존 후원번호·리워드 행을 업데이트합니다. 코드는 발급하지 않습니다.")
    @PostMapping("/funding-orders/tumblbug/import-edited")
    public ResponseEntity<ApiResponse<TumblbugSupporterImportResponse>> importEditedTumblbugRows(
            @Valid @RequestBody TumblbugSupporterRowsImportRequest request,
            @AuthenticationPrincipal Long adminUserId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.onSuccess(fundingAdminService.importTumblbugRows(request, adminUserId)));
    }

    @Operation(summary = "텀블벅 구매자 수동 등록", description = "엑셀 명단과 같은 필드의 구매자·주문 정보를 저장합니다. 이용권 코드는 발급하지 않습니다.")
    @PostMapping("/funding-orders/tumblbug/manual")
    public ResponseEntity<ApiResponse<ManualTumblbugBuyerResponse>> createManualTumblbugBuyer(
            @Valid @RequestBody ManualTumblbugBuyerRequest request,
            @AuthenticationPrincipal Long adminUserId
    ) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.onSuccess(fundingAdminService.createManualTumblbugBuyer(request, adminUserId)));
    }

    @Operation(summary = "텀블벅 구매자 정보 수정", description = "수동 수정 시에도 엑셀 가져오기와 동일한 원본 열과 옵션으로 주문 정보를 갱신합니다.")
    @PatchMapping("/funding-orders/tumblbug/{fundingOrderId}")
    public ResponseEntity<ApiResponse<ManualTumblbugBuyerResponse>> updateManualTumblbugBuyer(
            @PathVariable Long fundingOrderId,
            @Valid @RequestBody ManualTumblbugBuyerRequest request,
            @AuthenticationPrincipal Long adminUserId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.onSuccess(fundingAdminService.updateManualTumblbugBuyer(fundingOrderId, request, adminUserId)));
    }

    @Operation(summary = "텀블벅 디지털 선물 전달 결과 기록", description = "텀블벅 텍스트 선물 엑셀 업로드 후 전달 완료/실패를 후원번호 목록 단위로 기록합니다. 실제 텀블벅 업로드는 관리자가 수행합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전달 결과 반영"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "전달 상태 또는 실패 사유 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "캠페인 없음")
    })
    @PatchMapping("/funding-orders/tumblbug/digital-gifts/delivery")
    public ApiResponse<TumblbugDigitalGiftDeliveryUpdateResponse> updateTumblbugDigitalGiftDelivery(
            @Valid @RequestBody TumblbugDigitalGiftDeliveryUpdateRequest request,
            @AuthenticationPrincipal Long adminUserId
    ) {
        return ApiResponse.onSuccess(fundingAdminService.updateTumblbugDigitalGiftDelivery(request, adminUserId));
    }

    private byte[] readXlsx(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_TOO_LARGE);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
        }
    }

}
