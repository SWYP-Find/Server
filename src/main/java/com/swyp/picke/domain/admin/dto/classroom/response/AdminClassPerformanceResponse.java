package com.swyp.picke.domain.admin.dto.classroom.response;

import com.swyp.picke.domain.classroom.enums.ClassReportStatus;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.global.common.response.PageResponse;
import java.time.LocalDateTime;
import java.util.List;

/** 클래스 성과. 클래스 기본 정보는 클래스 상세 API 를 함께 쓴다. */
public record AdminClassPerformanceResponse(
        Long classId,
        String ownerEmail,
        int participantCount,
        int capacity,
        // 배틀을 시작한 참가자 수 (사전 투표 화면 진입 이상)
        long battleStartedCount,
        // 리포트 대상 참가자 수 (활동 중인 참가자, 운영자는 참여할 때만)
        long reportTargetCount,
        // 개인 리포트가 완료된 참가자 수 (참가자별 1건, 재생성 제외)
        long reportCompletedCount,
        // reportCompletedCount / reportTargetCount × 100. 대상이 없으면 null
        Double reportCompletionRate,
        long aiErrorCount,
        List<AdminAiErrorRow> aiErrors,
        PageResponse<Participant> participants
) {
    public record Participant(
            Long memberId,
            String userTag,
            String displayName,
            boolean owner,
            // 공개된 의견 + 숨기지 않은 댓글 수
            long commentCount,
            UserBattleStep battleStep,
            // 개인 리포트 상태. 아직 만들지 않았으면 null
            ClassReportStatus reportStatus,
            LocalDateTime reportGeneratedAt
    ) {}
}
