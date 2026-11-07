package com.swyp.picke.domain.classroom.dto.request;

import com.swyp.picke.domain.user.enums.PhilosopherType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 길이·필수값은 bean validation 대신 서비스가 필드별 issues 로 돌려준다 (부록 C "필드별 오류").
 * 카테고리·수준은 바탕 버전 값을 그대로 쓴다.
 */
@Schema(description = "주제 직접 수정 검증 요청. 저장된 주제 버전을 바탕으로 질문·설명·양측 선택지를 함께 보낸다")
public record ClassTopicValidationRequest(

        @Schema(description = "수정의 바탕이 된 주제 버전 ID", example = "12")
        @NotNull
        Long baseVersionId,

        @Schema(description = "질문 (60자 이하)", example = "표현하지 않은 감정까지 타인이 판단해도 될까요?")
        String question,

        @Schema(description = "설명 (80자 이하)", example = "감정 표현의 개인적 자유와 공동체의 기대를 비교해 봐요.")
        String description,

        @Schema(description = "선택지 2개 (A, B 순서)")
        @NotNull
        @Size(min = 2, max = 2)
        List<@Valid Option> options
) {

    public record Option(

            @Schema(description = "선택 문구 (20자 이하)", example = "판단할 수 있다")
            String label,

            @Schema(description = "관점 설명 (40자 이하)", example = "표현은 관계에 대한 책임의 일부다")
            String rationale,

            @Schema(description = "관점을 대변할 철학자. 비우면 바탕 버전의 철학자를 그대로 쓴다", example = "KANT", nullable = true)
            PhilosopherType philosopher
    ) {
    }
}
