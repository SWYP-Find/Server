package com.swyp.picke.domain.classroom.controller;

import com.swyp.picke.domain.classroom.dto.request.ClassCreateRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassUpdateRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDetailResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassSummaryResponse;
import com.swyp.picke.domain.classroom.service.ClassroomService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "클래스 API", description = "이용권 보유자가 만드는 클래스의 생성/조회/수정/삭제")
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;

    @Operation(summary = "클래스 생성",
            description = "사용 중인 이용권이 있어야 합니다(없으면 403 CLASS_403_TICKET). 정원은 이용권 인원을 그대로 씁니다. "
                    + "마감일은 비우면 이용권 종료일까지이고, 설정 시 현재 이후 ~ 이용권 종료일 범위만 허용합니다(400 CLASS_400_DEADLINE).")
    @PostMapping
    public ApiResponse<ClassDetailResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ClassCreateRequest request
    ) {
        return ApiResponse.onSuccess(classroomService.create(userId, request));
    }

    @Operation(summary = "내 클래스 목록", description = "내가 운영하는 클래스 + 참여 중인 클래스. 최신 생성순.")
    @GetMapping
    public ApiResponse<List<ClassSummaryResponse>> getMyClasses(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(classroomService.getMyClasses(userId));
    }

    @Operation(summary = "클래스 상세",
            description = "운영자 또는 참여 중인 멤버만 조회할 수 있습니다(403 CLASS_403_MEMBER). 초대코드는 운영자에게만 내려갑니다.")
    @GetMapping("/{classId}")
    public ApiResponse<ClassDetailResponse> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(classroomService.getDetail(userId, classId));
    }

    @Operation(summary = "클래스 수정 (운영자)",
            description = "이름 변경, 마감일 연장만 가능합니다. null 필드는 변경하지 않습니다. "
                    + "마감된 클래스는 수정할 수 없습니다(409 CLASS_409_CLOSED).")
    @PatchMapping("/{classId}")
    public ApiResponse<ClassDetailResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId,
            @Valid @RequestBody ClassUpdateRequest request
    ) {
        return ApiResponse.onSuccess(classroomService.update(userId, classId, request));
    }

    @Operation(summary = "클래스 삭제 (운영자)", description = "소프트 삭제. 이후 목록·상세에서 보이지 않습니다.")
    @DeleteMapping("/{classId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        classroomService.delete(userId, classId);
        return ApiResponse.onSuccess(null);
    }
}
