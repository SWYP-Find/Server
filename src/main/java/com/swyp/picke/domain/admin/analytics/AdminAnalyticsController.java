package com.swyp.picke.domain.admin.analytics;

import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자 지표", description = "Mixpanel 이벤트와 Sentry 오류를 관리자 화면에서 함께 본다")
public class AdminAnalyticsController {

    private static final long MAX_DAYS = 366;

    private final MixpanelClient mixpanelClient;
    private final SentryClient sentryClient;

    @GetMapping("/mixpanel")
    @Operation(summary = "Mixpanel 이벤트 일별 발생 수",
               description = "events 를 비우면 기간에 나타난 이벤트 전부를 센다. 원본 이벤트를 받아 세므로 기간은 31일까지")
    public ApiResponse<MixpanelEventReport> mixpanel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) List<String> events) {
        validateRange(from, to, MixpanelClient.MAX_DAYS);
        return ApiResponse.onSuccess(mixpanelClient.fetchDailyCounts(events, from, to));
    }

    @GetMapping("/sentry")
    @Operation(summary = "Sentry 미해결 이슈 상위 목록",
               description = "이벤트 수 내림차순 최대 20건. 토큰·프로젝트 설정이 없으면 NOT_CONFIGURED")
    public ApiResponse<SentryIssueReport> sentry(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to, MAX_DAYS);
        return ApiResponse.onSuccess(sentryClient.fetchUnresolvedIssues(from, to));
    }

    private void validateRange(LocalDate from, LocalDate to, long maxDays) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days < 1 || days > maxDays) {
            throw new IllegalArgumentException("조회 기간은 1일부터 " + maxDays + "일까지입니다.");
        }
    }
}
