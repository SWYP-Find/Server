package com.swyp.picke.domain.ticket.controller;

import com.swyp.picke.domain.ticket.dto.request.ClassTicketRegisterRequest;
import com.swyp.picke.domain.ticket.dto.response.ClassTicketResponse;
import com.swyp.picke.domain.ticket.service.ClassTicketService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "클래스 이용권 API", description = "펀딩으로 받은 이용권 코드 등록 및 내 이용권 조회")
@RestController
@RequestMapping("/api/v1/class-tickets")
@RequiredArgsConstructor
public class ClassTicketController {

    private final ClassTicketService classTicketService;

    @Operation(summary = "이용권 코드 등록",
            description = "코드를 등록하면 즉시 ACTIVE 가 되고 6개월 뒤 만료됩니다. 계정당 사용 중인 이용권은 1개입니다.")
    @PostMapping("/register")
    public ApiResponse<ClassTicketResponse> register(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ClassTicketRegisterRequest request
    ) {
        return ApiResponse.onSuccess(classTicketService.register(userId, request));
    }

    @Operation(summary = "내 이용권 조회",
            description = "사용 중(ACTIVE) 이용권, 없으면 가장 최근 만료(EXPIRED) 이용권을 돌려줍니다. 둘 다 없으면 404(TICKET_404).")
    @GetMapping("/me")
    public ApiResponse<ClassTicketResponse> getMyTicket(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(classTicketService.getMyTicket(userId));
    }
}
