package com.swyp.picke.domain.ticket.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FundingCsvParserTest {

    private static final String HEADER =
            "platform,campaign_id,order_id,reward_tier,quantity,supporter_name,supporter_email,supporter_phone,payment_status,paid_at";

    private static FundingCsvParser.Result parse(String csv) {
        return FundingCsvParser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("BOM·CRLF·따옴표 안 콤마·숫자 reward_tier·여러 날짜 형식을 받아들인다")
    void parsesVariousShapes() {
        String csv = "﻿" + HEADER + "\r\n"
                + "TUMBLBUG,camp-1,ORD-1,SEAT_15,1,\"홍, 길동\",hong@test.com,010-1111-2222,PAID_CONFIRMED,2026-10-01 12:34:56\r\n"
                + "tumblbug,camp-1,ORD-2,30,1,김후원,,,paid_confirmed,2026-10-02T09:00:00\r\n"
                + "WADIZ,camp-2,ORD-3,5,2,이후원,lee@test.com,,PAID_CONFIRMED,2026-10-03\r\n"
                + ",,,,,,,,,\r\n"
                + "MANUAL,camp-3,ORD-4,SEAT_5,1,박대기,,,PAYMENT_PENDING,\r\n";

        FundingCsvParser.Result result = parse(csv);

        assertThat(result.totalRows()).isEqualTo(4);
        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).hasSize(4);

        FundingCsvParser.Row first = result.rows().get(0);
        assertThat(first.line()).isEqualTo(2);
        assertThat(first.platform()).isEqualTo(FundingPlatform.TUMBLBUG);
        assertThat(first.rewardTier()).isEqualTo(RewardTier.SEAT_15);
        assertThat(first.supporterName()).isEqualTo("홍, 길동");
        assertThat(first.paidAt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 12, 34, 56));

        assertThat(result.rows().get(1).rewardTier()).isEqualTo(RewardTier.SEAT_30);
        assertThat(result.rows().get(1).supporterEmail()).isNull();
        assertThat(result.rows().get(1).paidAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 9, 0));
        assertThat(result.rows().get(2).quantity()).isEqualTo(2);
        assertThat(result.rows().get(2).paidAt()).isEqualTo(LocalDateTime.of(2026, 10, 3, 0, 0));
        assertThat(result.rows().get(3).paymentStatus()).isEqualTo(PaymentStatus.PAYMENT_PENDING);
        assertThat(result.rows().get(3).paidAt()).isNull();
    }

    @Test
    @DisplayName("행 단위 오류는 줄 번호·order_id·사유로 모으고 나머지 행은 정상 반환한다")
    void collectsRowErrors() {
        String csv = HEADER + "\n"
                + "TUMBLBUG,c,ORD-OK,SEAT_5,1,정상,,,PAID_CONFIRMED,2026-10-01 00:00:00\n"
                + "TUMBLBUG,c,ORD-TIER,SEAT_99,1,티어오류,,,PAID_CONFIRMED,2026-10-01 00:00:00\n"
                + "TUMBLBUG,c,ORD-NOPAID,SEAT_5,1,결제일없음,,,PAID_CONFIRMED,\n"
                + "TUMBLBUG,c,ORD-OK,SEAT_5,1,중복,,,PAID_CONFIRMED,2026-10-01 00:00:00\n"
                + "TUMBLBUG,c,,SEAT_5,1,주문번호없음,,,PAID_CONFIRMED,2026-10-01 00:00:00\n"
                + "TUMBLBUG,c,ORD-QTY,SEAT_5,zero,수량오류,,,PAID_CONFIRMED,2026-10-01 00:00:00\n"
                + "TUMBLBUG,c,ORD-DATE,SEAT_5,1,날짜오류,,,PAID_CONFIRMED,10/01/2026\n";

        FundingCsvParser.Result result = parse(csv);

        assertThat(result.totalRows()).isEqualTo(7);
        assertThat(result.rows()).extracting(FundingCsvParser.Row::orderId).containsExactly("ORD-OK");
        assertThat(result.errors()).hasSize(6);
        assertThat(result.errors()).extracting(FundingCsvParser.RowError::line).containsExactly(3, 4, 5, 6, 7, 8);
        assertThat(result.errors().get(0).reason()).contains("reward_tier");
        assertThat(result.errors().get(1).reason()).contains("paid_at");
        assertThat(result.errors().get(2).reason()).contains("두 번 이상");
        assertThat(result.errors().get(3).reason()).contains("order_id");
        assertThat(result.errors().get(4).reason()).contains("quantity");
        assertThat(result.errors().get(5).reason()).contains("paid_at");
    }

    @Test
    @DisplayName("헤더가 템플릿과 다르거나 따옴표가 닫히지 않으면 FUNDING_400_CSV")
    void rejectsInvalidFile() {
        assertThatThrownBy(() -> parse("platform,order_id\nTUMBLBUG,ORD-1\n"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FUNDING_CSV_INVALID);

        assertThatThrownBy(() -> parse(HEADER + "\nTUMBLBUG,c,ORD-1,SEAT_5,1,\"열린 따옴표,,,PAID_CONFIRMED,2026-10-01\n"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FUNDING_CSV_INVALID);

        assertThatThrownBy(() -> parse(""))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FUNDING_CSV_INVALID);
    }
}
