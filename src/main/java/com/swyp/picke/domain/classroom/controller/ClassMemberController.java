package com.swyp.picke.domain.classroom.controller;

import com.swyp.picke.domain.classroom.dto.request.ClassJoinRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassMemberNameUpdateRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDetailResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassMemberResponse;
import com.swyp.picke.domain.classroom.service.ClassMemberService;
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

@Tag(name = "클래스 멤버 API", description = "초대코드로 참여, 명단 조회, 내 이름 변경, 나가기, 운영자 내보내기")
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassMemberController {

    private final ClassMemberService classMemberService;

    @Operation(summary = "초대코드로 클래스 참여",
            description = "코드는 공백·하이픈 제거·대문자 변환 후 6자여야 합니다(아니면 400). "
                    + "없는 코드 404 CLASS_404_CODE, 이용권 종료 409 CLASS_409_TICKET_ENDED, 마감일 경과 409 CLASS_409_CLOSED, 정원 초과 409 CLASS_409_FULL, "
                    + "이름 중복 409 CLASS_409_NAME, 내 클래스 409 CLASS_409_OWNER, 내보내진 클래스 403 CLASS_403_REMOVED. "
                    + "이미 참여 중이면 거절하지 않고 기존 상세를 그대로 돌려줍니다.")
    @PostMapping("/join")
    public ApiResponse<ClassDetailResponse> join(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ClassJoinRequest request
    ) {
        return ApiResponse.onSuccess(classMemberService.join(userId, request));
    }

    @Operation(summary = "클래스 명단",
            description = "운영자와 참여 중인 멤버만 볼 수 있습니다(403 CLASS_403_MEMBER). 운영자 행이 먼저, 이후 참여 순. "
                    + "표시 이름·역할·아바타만 내려가고 계정 식별자는 내려가지 않습니다.")
    @GetMapping("/{classId}/members")
    public ApiResponse<List<ClassMemberResponse>> getMembers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(classMemberService.getMembers(userId, classId));
    }

    @Operation(summary = "내 표시 이름 변경",
            description = "참여 중인 멤버 본인만. 같은 클래스의 다른 멤버와 같은 이름은 409 CLASS_409_NAME.")
    @PatchMapping("/{classId}/members/me")
    public ApiResponse<ClassMemberResponse> changeMyDisplayName(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId,
            @Valid @RequestBody ClassMemberNameUpdateRequest request
    ) {
        return ApiResponse.onSuccess(classMemberService.changeMyDisplayName(userId, classId, request));
    }

    @Operation(summary = "클래스 나가기",
            description = "운영자는 나갈 수 없습니다(403 CLASS_403_OWNER_LEAVE, 삭제로 종료). "
                    + "나간 뒤에는 같은 코드로 다시 참여할 수 있고, 활동 이력이 있으면 좌석은 반환되지 않습니다.")
    @DeleteMapping("/{classId}/members/me")
    public ApiResponse<Void> leave(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        classMemberService.leave(userId, classId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "멤버 내보내기 (운영자)",
            description = "내보내진 멤버는 같은 클래스에 다시 참여할 수 없습니다. 운영자 본인 행은 내보낼 수 없습니다(403 CLASS_403_OWNER_LEAVE).")
    @DeleteMapping("/{classId}/members/{memberId}")
    public ApiResponse<Void> removeMember(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId,
            @PathVariable Long memberId
    ) {
        classMemberService.removeMember(userId, classId, memberId);
        return ApiResponse.onSuccess(null);
    }
}
