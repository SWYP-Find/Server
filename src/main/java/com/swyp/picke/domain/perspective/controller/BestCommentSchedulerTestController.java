package com.swyp.picke.domain.perspective.controller;

import com.swyp.picke.domain.perspective.scheduler.BestCommentScheduler;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[Test] BestCommentScheduler", description = "스케줄러 테스트 API")
@RestController
@RequestMapping("/api/test/scheduler")
@RequiredArgsConstructor
public class BestCommentSchedulerTestController {

    private final BestCommentScheduler bestCommentScheduler;

    @Operation(summary = "베스트 댓글 정산 전체 실행", description = "PUBLISHED 상태 배틀 전체를 대상으로 베스트 댓글 포인트 정산을 즉시 실행합니다.")
    @PostMapping("/best-comment")
    public ApiResponse<String> runAll() {
        bestCommentScheduler.awardBestComments();
        return ApiResponse.onSuccess("베스트 댓글 정산 완료");
    }

    @Operation(summary = "베스트 댓글 정산 단건 실행", description = "특정 battleId에 대해서만 베스트 댓글 포인트 정산을 즉시 실행합니다.")
    @PostMapping("/best-comment/battles/{battleId}")
    public ApiResponse<String> runByBattle(@PathVariable Long battleId) {
        bestCommentScheduler.processBattle(battleId);
        return ApiResponse.onSuccess("battleId=" + battleId + " 베스트 댓글 정산 완료");
    }
}
