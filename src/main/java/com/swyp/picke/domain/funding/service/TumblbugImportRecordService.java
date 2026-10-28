package com.swyp.picke.domain.funding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.funding.dto.response.TumblbugOrderDetailResponse;
import com.swyp.picke.domain.funding.repository.TumblbugImportRecordRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enriches existing order results with XLSX metadata; does not search orders or tickets. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TumblbugImportRecordService {
    private final TumblbugImportRecordRepository importRecordRepository;
    private final ObjectMapper objectMapper;

    public Map<Long, TumblbugOrderDetailResponse> getDetails(List<Long> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        Map<Long, TumblbugOrderDetailResponse> result = new HashMap<>();
        for (var record : importRecordRepository.findByFundingOrderIdIn(orderIds)) {
            try {
                JsonNode row = objectMapper.readTree(record.getSourceSnapshot());
                result.put(record.getFundingOrder().getId(), new TumblbugOrderDetailResponse(
                        record.getCampaign().getName(), record.getCampaign().getTumblbugPlan(),
                        record.getSupporterNumber(), record.getRewardLineId(),
                        date(row, "paidAt"), text(row, "rewardName"), text(row, "giftText"), number(row, "rewardAmount"),
                        number(row, "additionalSupportAmount"), number(row, "totalSupportAmount"),
                        row.hasNonNull("notificationRequested") ? row.get("notificationRequested").asBoolean() : null,
                        text(row, "notificationRequest"), text(row, "rawPaymentStatus"), text(row, "giftDeliveryStatus")));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Invalid stored Tumblbug snapshot", e);
            }
        }
        return result;
    }

    private String text(JsonNode row, String field) {
        return row.hasNonNull(field) ? row.get(field).asText() : null;
    }

    private Long number(JsonNode row, String field) {
        return row.hasNonNull(field) ? row.get(field).asLong() : null;
    }

    private java.time.LocalDateTime date(JsonNode row, String field) {
        if (!row.hasNonNull(field)) return null;
        try {
            return java.time.LocalDateTime.parse(row.get(field).asText());
        } catch (java.time.format.DateTimeParseException ignored) {
            return null;
        }
    }
}
