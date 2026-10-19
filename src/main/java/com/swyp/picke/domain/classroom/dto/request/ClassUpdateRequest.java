package com.swyp.picke.domain.classroom.dto.request;

import com.swyp.picke.domain.classroom.entity.Classroom;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 개설 후 수정 가능한 항목은 이름과 마감일 연장뿐. null 인 필드는 변경하지 않는다. */
@Schema(description = "클래스 수정 요청 (운영자 전용). null 필드는 변경 없음")
public record ClassUpdateRequest(

        @Schema(description = "변경할 클래스 이름 (1~20자)", example = "3학년 2반 철학 토론", nullable = true)
        @Size(min = 1, max = Classroom.NAME_MAX_LENGTH)
        String name,

        @Schema(description = "연장할 마감일. 기존 마감일보다 뒤, 이용권 종료일 이전이어야 한다",
                example = "2027-01-31T23:59:59", nullable = true)
        LocalDateTime deadline
) {
}
