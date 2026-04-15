package com.swyp.picke.domain.battle.controller;

import com.swyp.picke.domain.battle.dto.request.BattleProposalRequest;
import com.swyp.picke.domain.battle.dto.response.BattleProposalResponse;
import com.swyp.picke.domain.battle.service.BattleProposalService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "배틀 제안 API", description = "배틀 제안")
@RestController
@RequestMapping("/api/v1/battles")
@RequiredArgsConstructor
public class BattleProposalController {

    private final BattleProposalService battleProposalService;

    @Operation(summary = "배틀 주제 제안")
    @PostMapping("/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BattleProposalResponse> propose(@Valid @RequestBody BattleProposalRequest request) {
        return ApiResponse.onSuccess(battleProposalService.propose(request));
    }
}
