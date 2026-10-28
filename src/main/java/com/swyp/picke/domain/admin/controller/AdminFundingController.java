package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.ticket.enums.RegisterReminderStage;
import com.swyp.picke.domain.admin.dto.funding.response.RegisterReminderTargetResponse;
import com.swyp.picke.domain.admin.dto.funding.response.RegisterReminderResponse;
import com.swyp.picke.domain.admin.dto.funding.request.RegisterReminderRequest;
import com.swyp.picke.domain.admin.dto.funding.request.FundingRefundRequest;
import com.swyp.picke.domain.admin.dto.funding.request.TicketDeliveryRequest;
import com.swyp.picke.domain.admin.dto.funding.request.TicketIssueRequest;
import com.swyp.picke.domain.admin.dto.funding.response.FundingImportResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingOrderPageResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingRefundResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingSummaryResponse;
import com.swyp.picke.domain.admin.dto.funding.response.AdminTicketCodeResponse;
import com.swyp.picke.domain.admin.dto.funding.response.TicketDeliveryResponse;
import com.swyp.picke.domain.admin.dto.funding.response.TicketIssueResponse;
import com.swyp.picke.domain.admin.dto.funding.response.TumblbugCampaignDashboardResponse;
import com.swyp.picke.domain.admin.service.AdminFundingService;
import com.swyp.picke.domain.funding.enums.DeliveryStatus;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 어드민 펀딩 명단·이용권 운영. 모든 API 는 ROLE_ADMIN.
 * 이용권 코드 원문은 발급 응답 또는 별도 관리자 코드 조회 API 로만 노출됩니다.
 */
@Tag(name = "관리자 펀딩·이용권 API", description = "펀딩 명단 CSV 가져오기, 이용권 코드 발급·전달 기록·재발급·환불, 주문/이용권 현황")
@RestController
@RequestMapping("/api/v1/admin/funding")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFundingController {

    private final AdminFundingService adminFundingService;

    @Operation(summary = "펀딩 명단 CSV 가져오기",
            description = "UTF-8 CSV (헤더: platform, campaign_id, order_id, reward_tier, quantity, supporter_name, supporter_email, "
                    + "supporter_phone, payment_status, paid_at). order_id 기준으로 추가/갱신하며 여러 번 올려도 안전합니다. "
                    + "행 단위 오류는 errors 로 돌려주고 나머지는 반영합니다. REFUNDED/CANCELED 주문은 PAID_CONFIRMED 로 되돌리지 않습니다. "
                    + "헤더가 다르거나 파일을 읽을 수 없으면 400 FUNDING_400_CSV.")
    @PostMapping(value = "/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FundingImportResponse> importCsv(@RequestParam("file") MultipartFile file) {
        return ApiResponse.onSuccess(adminFundingService.importCsv(file));
    }

    @Operation(summary = "이용권 코드 발급",
            description = "fundingOrderIds 를 비우면 발급 가능한 전체 주문(결제 완료·미발급 수량 있음)에 발급합니다. "
                    + "주문 수량만큼 발급하며 이미 수량을 모두 발급한 주문은 건너뛰므로 재실행해도 중복 발급되지 않습니다. "
                    + "응답의 code 를 반환하고 서버는 해시와 암호문을 저장합니다.")
    @PostMapping("/tickets/issue")
    public ApiResponse<TicketIssueResponse> issueTickets(@RequestBody(required = false) TicketIssueRequest request) {
        return ApiResponse.onSuccess(adminFundingService.issueTickets(request));
    }

    @Operation(summary = "관리자 이용권 코드 조회", description = "대시보드에서 코드 전체 보기·복사할 때 호출합니다. ROLE_ADMIN 전용이며 조회 이력을 남깁니다.")
    @GetMapping("/tickets/{ticketId}/code")
    public org.springframework.http.ResponseEntity<ApiResponse<AdminTicketCodeResponse>> getTicketCode(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal Long adminUserId
    ) {
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(ApiResponse.onSuccess(adminFundingService.getTicketCode(ticketId, adminUserId)));
    }

    @Operation(summary = "이용권 전달 기록",
            description = "이메일/SMS/수동으로 코드를 전달한 결과를 기록합니다. SUCCESS 면 codeStatus 가 DELIVERED 로 바뀝니다. "
                    + "이미 등록(CLAIMED)됐거나 무효화(REVOKED)된 이용권이면 409 TICKET_409_STATE.")
    @PostMapping("/tickets/{ticketId}/deliveries")
    public ApiResponse<TicketDeliveryResponse> recordDelivery(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketDeliveryRequest request
    ) {
        return ApiResponse.onSuccess(adminFundingService.recordDelivery(ticketId, request));
    }

    @Operation(summary = "이용권 코드 재발급",
            description = "코드 분실 등으로 기존 코드를 무효화(REVOKED)하고 같은 주문·정원·등록기한으로 새 코드를 발급합니다. "
                    + "등록 전(ISSUED/DELIVERED)인 이용권만 가능. 이미 등록됨 409 TICKET_409_CLAIMED, 이미 무효화 409 TICKET_409_REVOKED, "
                    + "등록기한 경과 409 TICKET_409_REG_EXP.")
    @PostMapping("/tickets/{ticketId}/reissue")
    public ApiResponse<TicketIssueResponse.IssuedTicket> reissue(@PathVariable Long ticketId) {
        return ApiResponse.onSuccess(adminFundingService.reissue(ticketId));
    }

    @Operation(summary = "펀딩 환불 처리",
            description = "주문을 REFUNDED 로 바꾸고 살아 있는 이용권을 무효화(REVOKED)합니다. 이미 환불된 주문은 alreadyRefunded 로 돌려줍니다. "
                    + "없는 주문 ID 가 섞여 있으면 404 FUNDING_404_ORDER.")
    @PostMapping("/refunds")
    public ApiResponse<FundingRefundResponse> refund(@Valid @RequestBody FundingRefundRequest request) {
        return ApiResponse.onSuccess(adminFundingService.refund(request));
    }

    @Operation(summary = "펀딩 주문·이용권 목록",
            description = "주문과 살아 있는 이용권 요약 및 텀블벅 원본 정보(코드 원문·해시 제외). 주문번호·후원자명·이메일·후원번호 검색과 정원·전달 결과 필터를 지원합니다.")
    @GetMapping("/orders")
    public ApiResponse<FundingOrderPageResponse> getOrders(
            @RequestParam(required = false) String campaignId,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) TicketStatus ticketStatus,
            @RequestParam(required = false) TicketCodeStatus codeStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) DeliveryStatus deliveryStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(
                adminFundingService.getOrders(campaignId, paymentStatus, ticketStatus, codeStatus, keyword, capacity, deliveryStatus, page, size));
    }

    @Operation(summary = "텀블벅 캠페인 대시보드 통계",
            description = "캠페인의 후원 건수, 결제 완료, 전달 완료, 미전달 수를 조회합니다. 전달 완료는 최근 전달 기록이 성공인 주문 기준입니다.")
    @GetMapping("/orders/tumblbug/dashboard")
    public ApiResponse<TumblbugCampaignDashboardResponse> getTumblbugCampaignDashboard(
            @RequestParam String campaignId
    ) {
        return ApiResponse.onSuccess(adminFundingService.getTumblbugCampaignDashboard(campaignId));
    }

    @Operation(summary = "등록기한 임박 알림 대상",
            description = "전달 완료(DELIVERED)·미등록 이용권 중 등록기한이 D30(7일 초과~30일 이내) 또는 D7(7일 이내)에 들고, "
                    + "해당 단계 알림을 아직 SUCCESS 로 기록하지 않은 건을 등록기한 빠른 순으로 돌려줍니다. "
                    + "미등록 이용권은 앱 계정이 없어 푸시가 불가하므로, 운영자가 후원자 연락처로 보낸 뒤 결과를 기록합니다.")
    @GetMapping("/register-reminders")
    public ApiResponse<RegisterReminderTargetResponse> getRegisterReminderTargets(
            @RequestParam RegisterReminderStage stage
    ) {
        return ApiResponse.onSuccess(adminFundingService.getRegisterReminderTargets(stage));
    }

    @Operation(summary = "등록기한 임박 알림 발송 기록",
            description = "운영자가 보낸 등록기한 알림 결과를 단계별로 기록합니다. SUCCESS 로 기록한 단계는 대상 목록에서 빠집니다. "
                    + "전달 완료·미등록 이용권이 아니면 409 TICKET_409_STATE, 등록기한이 지났으면 409 TICKET_409_REG_EXP.")
    @PostMapping("/tickets/{ticketId}/register-reminders")
    public ApiResponse<RegisterReminderResponse> recordRegisterReminder(
            @PathVariable Long ticketId,
            @Valid @RequestBody RegisterReminderRequest request
    ) {
        return ApiResponse.onSuccess(adminFundingService.recordRegisterReminder(ticketId, request));
    }

    @Operation(summary = "펀딩·이용권 현황 요약", description = "결제 확정/발급 가능/보류/발급/미전달/전달/등록/환불/취소 카운트, 등록기한 알림 대상 수")
    @GetMapping("/summary")
    public ApiResponse<FundingSummaryResponse> getSummary() {
        return ApiResponse.onSuccess(adminFundingService.getSummary());
    }
}
