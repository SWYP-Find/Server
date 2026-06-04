package com.swyp.picke.domain.battle.controller;

import com.swyp.picke.domain.battle.dto.response.BattleProposalResponse;
import com.swyp.picke.domain.battle.dto.request.BattleProposalReviewRequest;
import com.swyp.picke.domain.battle.service.BattleProposalService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 배틀 제안 API", description = "주제 제안 목록 조회 및 채택/미채택 처리")
@RestController
@RequestMapping("/api/v1/admin/battles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBattleProposalController {

    private final BattleProposalService battleProposalService;

    @Operation(summary = "배틀 주제 제안 목록 조회")
    @GetMapping("/proposals")
    public ApiResponse<?> getProposals(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.onSuccess(battleProposalService.getProposals(page, size, status));
    }

    @Operation(summary = "배틀 주제 채택/미채택 처리")
    @PatchMapping("/proposals/{proposalId}")
    public ApiResponse<BattleProposalResponse> review(
            @PathVariable Long proposalId,
            @Valid @RequestBody BattleProposalReviewRequest request
    ) {
        return ApiResponse.onSuccess(battleProposalService.review(proposalId, request));
    }
}
