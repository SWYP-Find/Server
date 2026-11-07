package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.classroom.enums.ClassReportViewState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "운영자용 클래스 인사이트. 정량 통계는 /result 에서 따로 제공되고 여기는 AI 요약만")
public record ClassInsightResponse(
        @Schema(description = "클래스 ID") Long classId,
        @Schema(description = "상태. BEFORE_CLOSE / PENDING / GENERATING / SUCCEEDED / LIMITED(의견 작성자 3명 미만) / FAILED") ClassReportViewState state,
        @Schema(description = "FAILED 일 때 운영자가 재시도할 수 있는지") boolean retryable,
        @Schema(description = "스냅샷 기준 시각") LocalDateTime cutoffAt,
        @Schema(description = "생성 시각") LocalDateTime generatedAt,
        @Schema(description = "인사이트 본문 (200자 이하). FAILED 면 \"AI 요약을 준비하지 못했어요\"") String insightText,
        @Schema(description = "full / partial") String coverage,
        @Schema(description = "분량 제한으로 제외된 글 수") int excludedCount,
        @Schema(description = "인사이트가 근거로 삼은 공개 글 (1~5건)") List<PublicPost> evidence,
        @Schema(description = "운영자에게 추천하는 대표 의견 (최대 2건)") List<PublicPost> highlights
) {

    @Schema(description = "공개 글. 작성자 정보는 넣지 않는다 (익명)")
    public record PublicPost(
            @Schema(description = "PERSPECTIVE / COMMENT") String sourceType,
            Long id,
            Long optionId,
            String stance,
            String content,
            LocalDateTime createdAt
    ) {
    }
}
