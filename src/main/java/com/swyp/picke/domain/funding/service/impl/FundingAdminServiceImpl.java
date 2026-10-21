package com.swyp.picke.domain.funding.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.admin.service.AdminFundingService;
import com.swyp.picke.domain.admin.dto.funding.request.TicketIssueRequest;
import com.swyp.picke.domain.admin.dto.funding.request.TicketDeliveryRequest;
import com.swyp.picke.domain.funding.dto.request.*;
import com.swyp.picke.domain.funding.dto.response.*;
import com.swyp.picke.domain.funding.entity.AdminAuditLog;
import com.swyp.picke.domain.funding.entity.FundingCampaign;
import com.swyp.picke.domain.funding.entity.TumblbugImportRecord;
import com.swyp.picke.domain.funding.enums.FundingCampaignStatus;
import com.swyp.picke.domain.funding.enums.DeliveryStatus;
import com.swyp.picke.domain.funding.repository.AdminAuditLogRepository;
import com.swyp.picke.domain.funding.repository.FundingCampaignRepository;
import com.swyp.picke.domain.funding.repository.TumblbugImportRecordRepository;
import com.swyp.picke.domain.funding.service.*;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.DeliveryChannel;
import com.swyp.picke.domain.ticket.repository.FundingOrderRepository;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.global.common.exception.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FundingAdminServiceImpl implements FundingAdminService {
    private final FundingCampaignRepository fundingCampaignRepository;
    private final TumblbugImportRecordRepository importRecordRepository;
    private final FundingOrderRepository fundingOrderRepository;
    private final ClassTicketRepository classTicketRepository;
    private final AdminFundingService adminFundingService;
    private final AdminAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final TumblbugSupporterExcelProcessor tumblbugExcelProcessor;
    private final TumblbugDigitalGiftWorkbookWriter tumblbugWorkbookWriter;

    @Override
    @Transactional(readOnly = true)
    public TumblbugImportPreviewResponse previewTumblbugSupporters(TumblbugSupporterImportRequest request, byte[] file) {
        validateTumblbugImportRequest(request);
        return analyzeTumblbugWorkbook(request, file, true).preview();
    }

    @Override
    @Transactional(readOnly = true)
    public TumblbugWorkbookInspectResponse inspectTumblbugWorkbook(byte[] file) {
        if (file == null || file.length == 0) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
        if (file.length > 10 * 1024 * 1024) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_TOO_LARGE);
        return tumblbugExcelProcessor.inspect(file);
    }

    @Override
    @Transactional
    public byte[] issueTumblbugDigitalGiftCodes(TumblbugSupporterImportRequest request, byte[] file, Long adminUserId) {
        validateTumblbugImportRequest(request);
        // Serialize imports per campaign, then recheck duplicates inside the lock.
        FundingCampaign campaign = fundingCampaignRepository
                .findByPlatformAndExternalCampaignIdForUpdate(FundingPlatform.TUMBLBUG, request.campaignId().trim())
                .orElseGet(() -> fundingCampaignRepository.saveAndFlush(new FundingCampaign(
                        FundingPlatform.TUMBLBUG, request.campaignId().trim(), request.campaignName().trim())));
        ImportAnalysis analysis = analyzeTumblbugWorkbook(request, file, true);
        if (analysis.duplicateRows() > 0) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_DUPLICATE);
        if (analysis.invalidRows() > 0) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_ROWS_INVALID);
        if (analysis.importableRows().isEmpty()) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_EMPTY);

        Map<String, String> codes = new LinkedHashMap<>();
        for (PreparedSupporter row : analysis.importableRows()) {
            FundingOrder order = fundingOrderRepository.saveAndFlush(FundingOrder.builder()
                    .platform(FundingPlatform.TUMBLBUG).campaignId(request.campaignId().trim())
                    .orderId(internalOrderId(request, row.supporterNumber()))
                    .rewardTier(RewardTier.valueOf("SEAT_" + row.capacity())).quantity(row.quantity())
                    .supporterName(row.buyerName()).paymentStatus(row.paymentStatus()).paidAt(row.paidAt()).build());
            importRecordRepository.save(new TumblbugImportRecord(campaign, order, row.supporterNumber(),
                    effectiveRewardLineId(request), request.sourceVersion(), snapshot(row)));
            // The latest implementation deliberately holds quantities other than one.
            if (order.isIssuable() && order.getPaidAt() != null) {
                var issued = adminFundingService.issueTickets(new TicketIssueRequest(List.of(order.getId())));
                codes.put(row.supporterNumber(), issued.issued().getFirst().code());
            }
            writeAudit(adminUserId, "TUMBLBUG_ORDER_IMPORTED", "FUNDING_ORDER", order.getId(), null,
                    Map.of("supporterNumber", row.supporterNumber(), "rewardLineId", effectiveRewardLineId(request),
                            "quantity", row.quantity(), "paymentStatus", row.paymentStatus()));
        }
        return tumblbugWorkbookWriter.write(codes);
    }

    @Override
    @Transactional
    public TumblbugDigitalGiftDeliveryUpdateResponse updateTumblbugDigitalGiftDelivery(
            TumblbugDigitalGiftDeliveryUpdateRequest request, Long adminUserId) {
        if (request.status() == null || request.status() == DeliveryStatus.PENDING
                || (request.status() == DeliveryStatus.FAILED
                && (request.failedReason() == null || request.failedReason().isBlank()))) {
            throw new CustomException(ErrorCode.FUNDING_INVALID_DELIVERY_STATUS);
        }
        FundingCampaign campaign = fundingCampaignRepository.findByPlatformAndExternalCampaignIdForUpdate(
                FundingPlatform.TUMBLBUG, request.campaignId().trim())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_CAMPAIGN_NOT_FOUND));
        List<String> numbers = request.supporterNumbers().stream().map(String::trim).distinct().toList();
        List<TumblbugImportRecord> records = importRecordRepository
                .findByCampaignIdAndSupporterNumberInAndRewardLineId(campaign.getId(), numbers, request.rewardLineId().trim());
        int updated = 0;
        for (TumblbugImportRecord record : records) {
            ClassTicket ticket = classTicketRepository.findByFundingOrderIdAndStatusNot(
                    record.getFundingOrder().getId(), TicketStatus.REVOKED).orElse(null);
            if (ticket == null || ticket.getCodeStatus() != TicketCodeStatus.ISSUED
                    || Objects.equals(record.getDeliveryRecordedTicketId(), ticket.getId())) continue;
            adminFundingService.recordDelivery(ticket.getId(), new TicketDeliveryRequest(
                    DeliveryChannel.TUMBLBUG_DIGITAL_GIFT,
                    request.status() == DeliveryStatus.SENT
                            ? com.swyp.picke.domain.ticket.enums.DeliveryStatus.SUCCESS
                            : com.swyp.picke.domain.ticket.enums.DeliveryStatus.FAILED,
                    request.failedReason()));
            record.recordDelivery(ticket.getId());
            updated++;
        }
        writeAudit(adminUserId, "TUMBLBUG_DELIVERY_RECORDED", "FUNDING_CAMPAIGN", campaign.getId(),
                request.failedReason(), Map.of("status", request.status(), "updated", updated));
        return new TumblbugDigitalGiftDeliveryUpdateResponse(numbers.size(), records.size(), updated,
                numbers.size() - records.size());
    }

    private String internalOrderId(TumblbugSupporterImportRequest request, String supporterNumber) {
        try {
            String identity = objectMapper.writeValueAsString(List.of(request.campaignId().trim(),
                    supporterNumber, effectiveRewardLineId(request)));
            return "TB-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8))).substring(0, 47);
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String snapshot(PreparedSupporter row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Tumblbug snapshot could not be serialized", e);
        }
    }

    private void writeAudit(Long adminUserId, String action, String targetType, Long targetId,
                            String reason, Map<String, ?> metadata) {
        try {
            auditLogRepository.save(new AdminAuditLog(adminUserId, action, targetType, targetId, reason,
                    objectMapper.writeValueAsString(metadata)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Funding audit metadata could not be serialized", e);
        }
    }

    private ImportAnalysis analyzeTumblbugWorkbook(
            TumblbugSupporterImportRequest request, byte[] file, boolean checkDuplicates) {
        if (file == null || file.length == 0) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
        if (file.length > 10 * 1024 * 1024) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_TOO_LARGE);

        List<TumblbugSupporterExcelProcessor.SupporterRow> sourceRows = tumblbugExcelProcessor.read(file, request.sheetName());
        if (sourceRows.isEmpty()) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_EMPTY);
        FundingCampaign existingCampaign = fundingCampaignRepository.findByPlatformAndExternalCampaignId(
                FundingPlatform.TUMBLBUG, request.campaignId().trim()).orElse(null);
        if (existingCampaign != null && existingCampaign.getStatus() != FundingCampaignStatus.ACTIVE) {
            throw new CustomException(ErrorCode.FUNDING_CAMPAIGN_INACTIVE);
        }

        List<PreparedSupporter> importableRows = new ArrayList<>();
        List<TumblbugImportRowResponse> previewRows = new ArrayList<>();
        Set<String> supporterNumbersInFile = new HashSet<>();
        int duplicateRows = 0;
        int invalidRows = 0;
        for (TumblbugSupporterExcelProcessor.SupporterRow source : sourceRows) {
            String supporterNumber = trimToNull(source.supporterNumber());
            String buyerName = trimToNull(source.buyerName());
            String invalidReason = null;
            if (supporterNumber == null) invalidReason = "후원번호가 비어 있습니다.";
            else if (supporterNumber.length() > 120) invalidReason = "후원번호가 허용 길이를 초과합니다.";
            else if (buyerName == null) invalidReason = "후원자 이름이 비어 있습니다.";
            else if (buyerName.length() > 50) invalidReason = "후원자 이름이 허용 길이를 초과합니다.";

            GiftInfo giftInfo = parseGiftInfo(source.gift());
            if (invalidReason == null && giftInfo == null) invalidReason = "선물 이름 또는 선물 금액을 읽을 수 없습니다.";
            if (invalidReason == null && giftInfo.rewardAmount() == null) invalidReason = "선물 셀에서 선물 금액을 읽을 수 없습니다.";
            if (invalidReason == null && giftInfo.rewardName().length() > 120) invalidReason = "선물 이름이 허용 길이를 초과합니다.";
            int quantity = giftInfo == null ? 1 : giftInfo.quantity();
            if (invalidReason == null && (quantity < 1 || quantity > 7)) {
                invalidReason = "수량은 1~7이어야 합니다. 텀블벅 텍스트 선물 300자 제한을 확인해 주세요.";
            }

            Long additionalSupportAmount = parseSupportAmount(source.additionalSupportAmount(), true);
            Long totalSupportAmount = parseSupportAmount(source.totalSupportAmount(), false);
            if (invalidReason == null && additionalSupportAmount == null) invalidReason = "추가 후원 금액을 읽을 수 없습니다.";
            if (invalidReason == null && totalSupportAmount == null) invalidReason = "총 후원 금액을 읽을 수 없습니다.";

            String rawPaymentStatus = trimToNull(source.paymentStatus());
            PaymentStatus paymentStatus = parseTumblbugPaymentStatus(rawPaymentStatus);
            if (invalidReason == null && paymentStatus == null) invalidReason = "결제 상태를 인식할 수 없습니다.";
            if (invalidReason == null && rawPaymentStatus.length() > 80) invalidReason = "결제 상태 값이 허용 길이를 초과합니다.";
            String notificationRequest = trimToNull(source.notificationRequest());
            Boolean notificationRequested = parseNotificationRequested(notificationRequest);
            if (invalidReason == null && notificationRequested == null) invalidReason = "알림 신청 상태를 인식할 수 없습니다.";
            if (invalidReason == null && notificationRequest.length() > 80) invalidReason = "알림 신청 값이 허용 길이를 초과합니다.";
            String giftDeliveryStatus = trimToNull(source.giftDeliveryStatus());
            if (invalidReason == null && giftDeliveryStatus == null) invalidReason = "선물 전달 상태가 비어 있습니다.";
            if (invalidReason == null && giftDeliveryStatus.length() > 80) invalidReason = "선물 전달 상태 값이 허용 길이를 초과합니다.";

            LocalDateTime paidAt = null;
            if (invalidReason == null) {
                try {
                    paidAt = parsePaidAt(source.supportedAt(), null);
                    if (paidAt == null && paymentStatus == PaymentStatus.PAID_CONFIRMED) {
                        invalidReason = "결제 완료 행의 후원 일시가 비어 있습니다.";
                    } else if (paidAt != null && paidAt.isAfter(LocalDateTime.now(ZoneId.of("Asia/Seoul")))) {
                        invalidReason = "후원 일시가 미래입니다.";
                    }
                } catch (DateTimeParseException e) {
                    invalidReason = "후원 일시 형식을 읽을 수 없습니다.";
                }
            }

            if (invalidReason != null) {
                invalidRows++;
                previewRows.add(importRow(source, supporterNumber, buyerName, giftInfo, additionalSupportAmount,
                        totalSupportAmount, paidAt, notificationRequest, quantity, "INVALID", invalidReason));
                continue;
            }

            boolean duplicateInFile = !supporterNumbersInFile.add(supporterNumber);
            boolean duplicateInServer = checkDuplicates && ((existingCampaign != null
                    && importRecordRepository.existsByCampaignIdAndSupporterNumberAndRewardLineId(
                            existingCampaign.getId(), supporterNumber, effectiveRewardLineId(request)))
                    || fundingOrderRepository.findByOrderId(supporterNumber).isPresent()
                    || fundingOrderRepository.findByOrderId(internalOrderId(request, supporterNumber)).isPresent());
            if (duplicateInFile || duplicateInServer) {
                duplicateRows++;
                previewRows.add(importRow(source, supporterNumber, buyerName, giftInfo, additionalSupportAmount,
                        totalSupportAmount, paidAt, notificationRequest, quantity, "DUPLICATE", "이미 등록된 후원번호와 리워드입니다."));
                continue;
            }

            String sourceSnapshot;
            try {
                sourceSnapshot = objectMapper.writeValueAsString(source.sourceColumns());
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
            }
            importableRows.add(new PreparedSupporter(source.rowNumber(), supporterNumber, buyerName,
                    trimToNull(source.gift()), giftInfo.rewardName(), giftInfo.rewardAmount(), additionalSupportAmount,
                    totalSupportAmount, quantity, request.capacity(), paymentStatus, paidAt,
                    notificationRequested, notificationRequest, rawPaymentStatus, giftDeliveryStatus,
                    sourceSnapshot));
            boolean issuable = paymentStatus == PaymentStatus.PAID_CONFIRMED && quantity == 1;
            String result = issuable ? "READY_CODE" : "SAVED_NO_CODE";
            String message = issuable
                    ? "표 정보를 저장하고 이용권 코드를 발급합니다."
                    : "표 정보를 저장하며 결제 미확정 또는 수량이 1이 아니므로 발급을 보류합니다.";
            previewRows.add(importRow(source, supporterNumber, buyerName, giftInfo, additionalSupportAmount,
                    totalSupportAmount, paidAt, notificationRequest, quantity, result, message));
        }
        int codeEligibleRows = (int) importableRows.stream()
                .filter(row -> row.paymentStatus() == PaymentStatus.PAID_CONFIRMED && row.quantity() == 1).count();
        TumblbugWorkbookInspectResponse.SheetSample sheetInfo = tumblbugExcelProcessor.inspect(file).sheets().stream()
                .filter(sheet -> sheet.sheetName().equals(request.sheetName())).findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID));
        TumblbugImportPreviewResponse preview = new TumblbugImportPreviewResponse(request.sheetName(),
                sheetInfo.headerRowNumber() == null ? 0 : sheetInfo.headerRowNumber(),
                sheetInfo.detectedColumns().keySet().stream().toList(), sourceRows.size(), importableRows.size(),
                codeEligibleRows, duplicateRows, invalidRows, List.copyOf(previewRows));
        return new ImportAnalysis(List.copyOf(importableRows), preview, duplicateRows, invalidRows);
    }

    private TumblbugImportRowResponse importRow(TumblbugSupporterExcelProcessor.SupporterRow source,
            String supporterNumber, String buyerName, GiftInfo giftInfo, Long additionalSupportAmount,
            Long totalSupportAmount, LocalDateTime supportedAt, String notificationRequest, Integer quantity,
            String result, String message) {
        return new TumblbugImportRowResponse(source.rowNumber(), supporterNumber, buyerName,
                giftInfo == null ? null : giftInfo.rewardName(), giftInfo == null ? null : giftInfo.rewardAmount(),
                additionalSupportAmount, totalSupportAmount, supportedAt, notificationRequest,
                source.paymentStatus(), source.giftDeliveryStatus(), quantity, result, message);
    }

    private void validateTumblbugImportRequest(TumblbugSupporterImportRequest request) {
        if (request == null || request.capacity() == null || !List.of(5, 15, 30).contains(request.capacity())) {
            throw new CustomException(ErrorCode.FUNDING_REWARD_UNSUPPORTED);
        }
    }

    private String effectiveRewardLineId(TumblbugSupporterImportRequest request) {
        String rewardLineId = trimToNull(request.rewardLineId());
        return rewardLineId == null ? request.sheetName().trim() : rewardLineId;
    }

    private GiftInfo parseGiftInfo(String gift) {
        if (gift == null || gift.isBlank()) return null;
        String normalized = gift.trim().replaceAll("\\s+", " ");
        int quantity = 1;
        java.util.regex.Matcher quantityMatcher = java.util.regex.Pattern
                .compile("(?i)\\(\\s*x\\s*(\\d+)\\s*\\)\\s*$").matcher(normalized);
        if (quantityMatcher.find()) {
            try {
                quantity = Integer.parseInt(quantityMatcher.group(1));
                normalized = normalized.substring(0, quantityMatcher.start()).trim();
            } catch (NumberFormatException e) {
                return null;
            }
        }
        java.util.regex.Matcher amountMatcher = java.util.regex.Pattern.compile("([0-9][0-9,]*)\\s*원").matcher(normalized);
        Long rewardAmount = null;
        if (amountMatcher.find()) {
            try {
                rewardAmount = Long.parseLong(amountMatcher.group(1).replace(",", ""));
                normalized = amountMatcher.replaceFirst(" ").trim();
            } catch (NumberFormatException e) {
                return null;
            }
        }
        String rewardName = normalized.replaceAll("[|·-]", " ").replaceAll("\\s+", " ").trim();
        if (rewardName.isBlank()) return null;
        return new GiftInfo(rewardName, rewardAmount, quantity);
    }

    private Long parseSupportAmount(String rawValue, boolean blankIsZero) {
        if (rawValue == null || rawValue.isBlank()) return blankIsZero ? 0L : null;
        String digits = rawValue.trim().replaceAll("[^0-9-]", "");
        if (digits.isBlank() || digits.equals("-")) return blankIsZero ? 0L : null;
        try {
            long value = Long.parseLong(digits);
            return value < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private PaymentStatus parseTumblbugPaymentStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) return null;
        String normalized = rawStatus.replaceAll("\\s+", "").toUpperCase();
        if (normalized.equals("결제완료") || normalized.equals("PAID_CONFIRMED")) return PaymentStatus.PAID_CONFIRMED;
        if (normalized.contains("환불") || normalized.equals("REFUNDED")) return PaymentStatus.REFUNDED;
        if (normalized.contains("취소") || normalized.equals("CANCELED")) return PaymentStatus.CANCELED;
        if (normalized.contains("실패") || normalized.equals("PAYMENT_FAILED")) return PaymentStatus.PAYMENT_FAILED;
        if (normalized.contains("대기") || normalized.contains("예정") || normalized.equals("PAYMENT_PENDING")) {
            return PaymentStatus.PAYMENT_PENDING;
        }
        return null;
    }

    private Boolean parseNotificationRequested(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return null;
        String normalized = rawValue.replaceAll("\\s+", "").toLowerCase();
        if (normalized.contains("안함") || normalized.contains("미신청") || normalized.equals("n")
                || normalized.equals("no") || normalized.equals("false")) return false;
        if (normalized.contains("신청") || normalized.equals("y") || normalized.equals("yes")
                || normalized.equals("true")) return true;
        return null;
    }

    private LocalDateTime parsePaidAt(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
            // Excel exports can use locale-formatted date cells instead of ISO timestamps.
        }
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported export format.
            }
        }
        try {
            return java.time.LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            throw new DateTimeParseException("Unsupported Tumblbug date", normalized, 0);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record GiftInfo(String rewardName, Long rewardAmount, int quantity) { }

    private record PreparedSupporter(int rowNumber, String supporterNumber, String buyerName,
                                     String giftText, String rewardName, Long rewardAmount,
                                     Long additionalSupportAmount, Long totalSupportAmount,
                                     int quantity, int capacity, PaymentStatus paymentStatus,
                                     LocalDateTime paidAt, Boolean notificationRequested,
                                     String notificationRequest, String rawPaymentStatus,
                                      String giftDeliveryStatus, String sourceSnapshot) { }

    private record ImportAnalysis(List<PreparedSupporter> importableRows,
                                  TumblbugImportPreviewResponse preview,
                                  int duplicateRows, int invalidRows) { }

}
