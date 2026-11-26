package com.swyp.picke.domain.admin.dto.classroom.response;

import java.time.LocalDateTime;
import java.util.List;

/** 이용권 계정 상세의 이용 현황. 연결된(삭제되지 않은) 클래스만 센다. */
public record AdminTicketUsageResponse(
        // 이 이용권으로 만든 클래스 수 (누적)
        long classCount,
        // 클래스별 좌석 점유 인원 합
        long participantCount,
        // 배틀을 끝까지 마친 기록 수 (참가자·클래스 단위)
        long battleCompletedCount,
        // 최근 30일 안에 완료된 리포트 수 (개인 리포트 + 운영자 인사이트)
        long reportsLast30Days,
        // 연결된 클래스에서 마지막으로 활동이 있던 시각. 활동이 없으면 null
        LocalDateTime lastActiveAt,
        List<ClassUsage> classes
) {
    public record ClassUsage(
            Long classId,
            String name,
            AdminClassState state,
            int participantCount,
            int capacity,
            // AI 토론을 시작한 횟수
            long debateCount,
            long battleCompletedCount,
            // 완료된 개인 리포트 수
            long reportCount,
            LocalDateTime lastActiveAt,
            LocalDateTime createdAt
    ) {}
}
