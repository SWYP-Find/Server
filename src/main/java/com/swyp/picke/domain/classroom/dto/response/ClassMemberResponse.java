package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 명단 화면(S12)용. 계정 식별자(userId·태그)는 내려가지 않고 클래스 안의 표시 이름만 보인다. */
@Schema(description = "클래스 멤버")
public record ClassMemberResponse(
        @Schema(description = "멤버십 ID (내보내기 등 멤버 단위 작업에 쓴다)") Long memberId,
        @Schema(description = "클래스 안에서 보이는 이름") String displayName,
        @Schema(description = "역할 (OWNER / MEMBER)") ClassMemberRole role,
        @Schema(description = "아바타 이미지 URL") String characterUrl,
        @Schema(description = "조회한 사람 본인이면 true (연필 버튼 노출용)") boolean me,
        @Schema(description = "참여 시각") LocalDateTime joinedAt
) {
}
