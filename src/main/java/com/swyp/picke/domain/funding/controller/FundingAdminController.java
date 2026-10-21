package com.swyp.picke.domain.funding.controller;

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
import java.nio.charset.StandardCharsets;
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

    @Operation(summary = "텀블벅 후원자 저장 및 코드 엑셀 생성", description = "자동 판별한 후원자 표 데이터를 DB에 저장하고 결제 완료 행의 이용권 코드를 텀블벅 업로드용 엑셀로 반환합니다. 원문 코드는 이 파일에만 포함됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "후원번호-이용권 코드 XLSX 다운로드",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "엑셀 또는 캠페인 옵션 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "필수 열 누락 또는 행 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 등록된 후원번호"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "엑셀 10MB 초과")
    })
    @PostMapping(value = "/funding-orders/tumblbug/digital-gifts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> issueTumblbugDigitalGiftCodes(
            @Valid @RequestPart("options") TumblbugSupporterImportRequest options,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long adminUserId
    ) {
        byte[] workbook = fundingAdminService.issueTumblbugDigitalGiftCodes(options, readXlsx(file), adminUserId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("picke-tumblbug-digital-gifts.xlsx", StandardCharsets.UTF_8).build());
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        headers.setPragma("no-cache");
        headers.setContentLength(workbook.length);
        return ResponseEntity.ok().headers(headers).body(workbook);
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
