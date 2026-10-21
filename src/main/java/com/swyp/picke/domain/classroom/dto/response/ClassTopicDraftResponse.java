package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.classroom.enums.ClassTopicSuggestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "커스텀 배틀 주제 초안. 제안이 실패해도 이전 성공 배치의 후보가 그대로 남는다")
public record ClassTopicDraftResponse(
        @Schema(description = "초안 ID") Long draftId,
        @Schema(description = "주제·관심사 (마지막 성공 배치 기준)") String topicSeed,
        BattleDifficulty difficulty,
        List<BattleCategory> categories,
        @Schema(description = "마지막 제안 결과. NONE = 아직 요청 전, GENERATING = 생성 중(\"생각을 정리하고 있어요\"), "
                + "SUCCEEDED = 성공, NEEDS_CLARIFICATION = 맥락 부족(clarificationMessage 표시), FAILED = 실패(기존 후보 유지, 횟수 차감 없음)")
        ClassTopicSuggestStatus suggestStatus,
        @Schema(description = "맥락 부족 시 보완 안내 문구", nullable = true) String clarificationMessage,
        @Schema(description = "현재 후보의 배치 번호 (성공 배치 수, 0 이면 후보 없음)") int batchNo,
        @Schema(description = "남은 제안 횟수 (최초 + 재추천 최대 3회)") int remainingSuggestions,
        @Schema(description = "남은 직접 수정 검증 횟수 (최대 3회)") int remainingValidations,
        @Schema(description = "현재 배치의 후보 8개") List<Candidate> candidates,
        @Schema(description = "가장 최근에 만든 검증된 주제 버전", nullable = true) ClassTopicVersionResponse latestVersion
) {

    public record Candidate(
            @Schema(description = "후보 ID (1~8)") int candidateId,
            String question,
            String description,
            BattleCategory category,
            BattleDifficulty difficulty,
            List<ClassTopicVersionResponse.Option> options
    ) {
    }
}
