package com.swyp.picke.domain.admin.dto.classroom.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 클래스 성과 분석. operation 은 전체 누적, 나머지는 from~to (한국 날짜, 양끝 포함) 기준. */
public record AdminClassAnalyticsResponse(
        LocalDateTime asOf,
        LocalDate from,
        LocalDate to,
        Operation operation,
        Flow flow,
        List<DailyCount> dailyReports,
        long aiErrorTotal,
        List<AdminAiErrorRow> aiErrors
) {
    public record Operation(
            // 회수(무효화) 포함 누적 발급
            long issuedTotal,
            long revokedCount,
            // 회수를 뺀 유효 발급
            long validIssued,
            // 유효 발급 중 계정에 등록된 이용권
            long registered,
            // registered / validIssued × 100. 유효 발급이 없으면 null
            Double registrationRate,
            // 지금 이용 기간 안인 이용권을 가진 계정 수 (만료 임박·클래스 미사용 포함)
            long activeAccounts
    ) {}

    public record Flow(
            // 기간 안에 리포트(개인·인사이트)가 1건 이상 완료된 클래스의 운영자 수
            long operatorsWithReport,
            // 기간 안에 성공한 AI 리포트 작업 수 (개인 리포트 + 인사이트, 재생성 포함)
            long aiReportsGenerated,
            // 기간 안에 시작한 클래스 배틀 수 (참가자·클래스 단위)
            long battlesStarted,
            // 그중 개인 리포트까지 완료된 수
            long battlesWithReport,
            Double battleToReportRate,
            // 기간 안에 완료된 리포트 수 (개인 + 인사이트)
            long reportsCompleted,
            // 그중 1회 이상 열람된 수
            long reportsViewed,
            Double reportViewRate
    ) {}

    public record DailyCount(LocalDate date, long count) {}
}
