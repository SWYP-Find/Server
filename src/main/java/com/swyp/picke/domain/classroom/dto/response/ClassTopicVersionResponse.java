package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.classroom.enums.ClassTopicVersionSource;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "검증된 주제 버전. 클래스 개설에는 이 topicVersionId 만 쓸 수 있다")
public record ClassTopicVersionResponse(
        @Schema(description = "주제 버전 ID (topic_version_id)") Long topicVersionId,
        @Schema(description = "초안 ID") Long draftId,
        @Schema(description = "초안 안의 버전 번호 (1부터)") int versionNo,
        @Schema(description = "CANDIDATE = AI 후보 선택, EDITED = 직접 수정 후 검증 통과") ClassTopicVersionSource source,
        String question,
        String description,
        BattleCategory category,
        BattleDifficulty difficulty,
        @Schema(description = "선택지 A, B") List<Option> options,
        LocalDateTime createdAt
) {

    @Schema(description = "선택지와 그 관점을 대변하는 철학자")
    public record Option(
            @Schema(description = "A / B") String position,
            @Schema(description = "선택 문구 (20자 이하)") String label,
            @Schema(description = "관점 설명 (40자 이하)") String rationale,
            @Schema(description = "철학자 코드") PhilosopherType philosopher,
            @Schema(description = "철학자 이름") String philosopherName,
            @Schema(description = "철학자 이미지 키") String philosopherImageKey
    ) {
    }
}
