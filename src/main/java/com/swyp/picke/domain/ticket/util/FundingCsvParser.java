package com.swyp.picke.domain.ticket.util;

import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 펀딩 명단 CSV 파서 (외부 라이브러리 없음). UTF-8(BOM 허용), 쌍따옴표 필드·필드 내 콤마·줄바꿈, CRLF 지원.
 * 헤더는 순서 무관하게 이름으로 매핑한다 (소문자·공백 무시). 템플릿 헤더:
 * platform, campaign_id, order_id, reward_tier, quantity, supporter_name, supporter_email, supporter_phone, payment_status, paid_at
 */
public final class FundingCsvParser {

    public static final List<String> REQUIRED_HEADERS = List.of(
            "platform", "campaign_id", "order_id", "reward_tier", "quantity",
            "supporter_name", "supporter_email", "supporter_phone", "payment_status", "paid_at");

    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private FundingCsvParser() {
    }

    /** 검증을 통과한 행 */
    public record Row(int line, FundingPlatform platform, String campaignId, String orderId, RewardTier rewardTier,
                      int quantity, String supporterName, String supporterEmail, String supporterPhone,
                      PaymentStatus paymentStatus, LocalDateTime paidAt) {
    }

    /** 행 단위 오류 (전체 실패가 아니라 응답에 모아서 돌려준다) */
    public record RowError(int line, String orderId, String reason) {
    }

    public record Result(List<Row> rows, List<RowError> errors, int totalRows) {
    }

    /** 파일 구조 자체가 잘못되면 FUNDING_400_CSV, 행 단위 문제는 errors 로 수집 */
    public static Result parse(InputStream in) {
        List<List<String>> records;
        try {
            records = readRecords(in);
        } catch (IOException | IllegalStateException e) {
            throw new CustomException(ErrorCode.FUNDING_CSV_INVALID);
        }
        if (records.isEmpty()) {
            throw new CustomException(ErrorCode.FUNDING_CSV_INVALID);
        }
        Map<String, Integer> columns = headerIndex(records.get(0));
        if (!columns.keySet().containsAll(REQUIRED_HEADERS)) {
            throw new CustomException(ErrorCode.FUNDING_CSV_INVALID);
        }

        List<Row> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        Set<String> seenOrderIds = new HashSet<>();
        int total = 0;
        for (int i = 1; i < records.size(); i++) {
            List<String> record = records.get(i);
            if (record.stream().allMatch(String::isBlank)) {
                continue;
            }
            total++;
            int line = i + 1;
            String orderId = value(record, columns, "order_id");
            try {
                Row row = toRow(line, record, columns);
                if (!seenOrderIds.add(row.orderId())) {
                    errors.add(new RowError(line, row.orderId(), "파일 안에 같은 order_id 가 두 번 이상 있습니다."));
                    continue;
                }
                rows.add(row);
            } catch (RowException e) {
                errors.add(new RowError(line, orderId == null ? null : orderId.trim(), e.getMessage()));
            }
        }
        return new Result(rows, errors, total);
    }

    private static Row toRow(int line, List<String> record, Map<String, Integer> columns) {
        FundingPlatform platform = parseEnum(FundingPlatform.class, required(record, columns, "platform"), "platform");
        String campaignId = required(record, columns, "campaign_id");
        String orderId = required(record, columns, "order_id");
        RewardTier rewardTier = parseRewardTier(required(record, columns, "reward_tier"));
        int quantity = parseQuantity(required(record, columns, "quantity"));
        String supporterName = required(record, columns, "supporter_name");
        String supporterEmail = optional(record, columns, "supporter_email");
        String supporterPhone = optional(record, columns, "supporter_phone");
        PaymentStatus paymentStatus = parseEnum(PaymentStatus.class, required(record, columns, "payment_status"), "payment_status");
        LocalDateTime paidAt = parseDateTime(optional(record, columns, "paid_at"));

        if (paymentStatus == PaymentStatus.PAID_CONFIRMED && paidAt == null) {
            throw new RowException("payment_status 가 PAID_CONFIRMED 이면 paid_at 이 필요합니다.");
        }
        if (orderId.length() > 100 || campaignId.length() > 100) {
            throw new RowException("order_id / campaign_id 는 100자 이하여야 합니다.");
        }
        if (supporterName.length() > 100 || (supporterEmail != null && supporterEmail.length() > 255)
                || (supporterPhone != null && supporterPhone.length() > 30)) {
            throw new RowException("supporter_name 100자, supporter_email 255자, supporter_phone 30자를 넘을 수 없습니다.");
        }
        return new Row(line, platform, campaignId, orderId, rewardTier, quantity, supporterName,
                supporterEmail, supporterPhone, paymentStatus, paidAt);
    }

    private static String required(List<String> record, Map<String, Integer> columns, String name) {
        String v = value(record, columns, name);
        if (v == null || v.isBlank()) {
            throw new RowException(name + " 값이 비어 있습니다.");
        }
        return v.trim();
    }

    private static String optional(List<String> record, Map<String, Integer> columns, String name) {
        String v = value(record, columns, name);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String value(List<String> record, Map<String, Integer> columns, String name) {
        Integer idx = columns.get(name);
        if (idx == null || idx >= record.size()) {
            return null;
        }
        return record.get(idx);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String name) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RowException(name + " 값이 올바르지 않습니다: " + raw);
        }
    }

    /** SEAT_5 | SEAT_15 | SEAT_30 또는 숫자 5 | 15 | 30 */
    private static RewardTier parseRewardTier(String raw) {
        String v = raw.trim().toUpperCase(Locale.ROOT);
        for (RewardTier tier : RewardTier.values()) {
            if (tier.name().equals(v) || String.valueOf(tier.getCapacity()).equals(v)) {
                return tier;
            }
        }
        throw new RowException("reward_tier 값이 올바르지 않습니다 (SEAT_5/SEAT_15/SEAT_30 또는 5/15/30): " + raw);
    }

    private static int parseQuantity(String raw) {
        int q;
        try {
            q = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new RowException("quantity 가 숫자가 아닙니다: " + raw);
        }
        if (q < 1) {
            throw new RowException("quantity 는 1 이상이어야 합니다.");
        }
        return q;
    }

    private static LocalDateTime parseDateTime(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        for (DateTimeFormatter f : DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(v, f);
            } catch (DateTimeParseException ignored) {
                // 다음 형식 시도
            }
        }
        try {
            return LocalDate.parse(v, DATE_FORMAT).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new RowException("paid_at 형식이 올바르지 않습니다 (yyyy-MM-dd HH:mm:ss): " + raw);
        }
    }

    private static Map<String, Integer> headerIndex(List<String> header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String name = header.get(i).replace("﻿", "").trim().toLowerCase(Locale.ROOT).replace(" ", "_");
            map.putIfAbsent(name, i);
        }
        return map;
    }

    /** RFC 4180 수준의 간단한 파서: 따옴표 필드 안의 콤마·줄바꿈·이중 따옴표("") 처리 */
    private static List<List<String>> readRecords(InputStream in) throws IOException {
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> fields = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean quoted = false;
            boolean first = true;
            int c;
            while ((c = reader.read()) != -1) {
                char ch = (char) c;
                if (first) {
                    first = false;
                    if (ch == '﻿') {
                        continue;
                    }
                }
                if (quoted) {
                    if (ch == '"') {
                        reader.mark(1);
                        int next = reader.read();
                        if (next == '"') {
                            field.append('"');
                        } else {
                            quoted = false;
                            if (next != -1) {
                                reader.reset();
                            }
                        }
                    } else {
                        field.append(ch);
                    }
                    continue;
                }
                switch (ch) {
                    case '"' -> quoted = true;
                    case ',' -> {
                        fields.add(field.toString());
                        field.setLength(0);
                    }
                    case '\r' -> {
                        // CRLF: '\n' 에서 행을 끝낸다
                    }
                    case '\n' -> {
                        fields.add(field.toString());
                        field.setLength(0);
                        records.add(fields);
                        fields = new ArrayList<>();
                    }
                    default -> field.append(ch);
                }
            }
            if (quoted) {
                throw new IllegalStateException("닫히지 않은 따옴표");
            }
            if (!fields.isEmpty() || field.length() > 0) {
                fields.add(field.toString());
                records.add(fields);
            }
        }
        return records;
    }

    private static final class RowException extends RuntimeException {
        RowException(String message) {
            super(message);
        }
    }
}
