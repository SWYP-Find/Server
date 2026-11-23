package com.swyp.picke.domain.admin.dto.classroom.request;

import com.swyp.picke.domain.classroom.entity.Classroom;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 관리자 수정도 앱 운영자와 같이 이름과 마감일 연장만 허용한다. null 은 변경하지 않는다. */
public record AdminClassUpdateRequest(
        @Schema(description = "클래스 이름 (1~20자)", nullable = true)
        @Size(min = 1, max = Classroom.NAME_MAX_LENGTH)
        String name,
        @Schema(description = "연장할 마감일. 현재 마감일 이후, 이용권 만료일 이내", nullable = true)
        LocalDateTime deadline
) {}
