package com.swyp.picke.domain.admin.dto.ad.request;

import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "제휴 광고 소재 등록/수정 요청")
public record AdCreativeRequest(

        @Schema(description = "매체", example = "COUPANG")
        @NotNull(message = "매체는 필수입니다.")
        AdNetwork network,

        @Schema(description = "노출 지면", example = "HOME_FEED")
        @NotNull(message = "노출 지면은 필수입니다.")
        AdSlotCode slot,

        @Schema(description = "배너 주 문구")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        String title,

        @Schema(description = "배너 보조 문구")
        @Size(max = 200, message = "보조 문구는 200자를 초과할 수 없습니다.")
        String subtitle,

        @Schema(description = "소재 이미지 URL")
        @NotBlank(message = "이미지 URL은 필수입니다.")
        @Size(max = 500, message = "이미지 URL은 500자를 초과할 수 없습니다.")
        String imageUrl,

        @Schema(description = "버튼 문구", example = "구매하러 가기")
        @NotBlank(message = "버튼 문구는 필수입니다.")
        @Size(max = 30, message = "버튼 문구는 30자를 초과할 수 없습니다.")
        String ctaText,

        @Schema(description = "각 매체 콘솔에서 발급한 제휴 링크")
        @NotBlank(message = "제휴 링크는 필수입니다.")
        @Size(max = 1000, message = "제휴 링크는 1000자를 초과할 수 없습니다.")
        String landingUrl,

        @Schema(description = "게재 상태", example = "ACTIVE")
        @NotNull(message = "게재 상태는 필수입니다.")
        AdStatus status,

        @Schema(description = "가중 로테이션 가중치. 클수록 자주 노출된다.", example = "1")
        @Positive(message = "가중치는 1 이상이어야 합니다.")
        Integer weight,

        @Schema(description = "게재 시작 시각. 비우면 즉시 시작")
        LocalDateTime startsAt,

        @Schema(description = "게재 종료 시각. 비우면 무제한")
        LocalDateTime endsAt
) {

    /**
     * 시작이 종료보다 뒤면 어떤 소재도 노출되지 않는다.
     * 저장은 되고 광고만 조용히 안 나가는 상황이라 등록 시점에 막는다.
     */
    @AssertTrue(message = "게재 종료 시각은 시작 시각보다 뒤여야 합니다.")
    @Schema(hidden = true)
    public boolean isPeriodValid() {
        if (startsAt == null || endsAt == null) {
            return true;
        }
        return !startsAt.isAfter(endsAt);
    }
}
