package com.swyp.picke.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "텀블벅 엑셀 업로드 및 내부 이용권 등록 옵션. 엑셀 열은 서버가 자동 판별합니다.")
public record TumblbugSupporterImportRequest(
        @NotBlank @Size(max = 50) @Schema(description = "픽케 캠페인 식별자", example = "picke-class-2026") String campaignId,
        @NotBlank @Size(max = 120) @Schema(description = "캠페인명", example = "픽케 클래스 펀딩") String campaignName,
        @NotBlank @Size(max = 31) @Schema(description = "처리할 리워드 시트 이름. 열 제목은 자동 판별") String sheetName,
        @Size(max = 120) @Schema(description = "픽케 내부 중복 방지용 리워드 키. 생략하면 시트명을 사용", example = "class-ticket-15") String rewardLineId,
        @NotNull @Min(5) @Max(30) @Schema(description = "리워드에 대응하는 이용권 정원. 5, 15, 30", allowableValues = {"5", "15", "30"}, example = "15") Integer capacity,
        @Size(max = 50) @Schema(description = "원본 파일명 또는 내보내기 버전. 미입력 시 서버가 생성") String sourceVersion
) {
}
