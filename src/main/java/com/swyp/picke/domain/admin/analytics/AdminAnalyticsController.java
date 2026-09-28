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
@Tag(name = "관리자 지표", description = "Mixpanel 이벤트와 Sentry 사용자 이벤트·오류를 관리자 화면에서 함께 본다")
public class AdminAnalyticsController {

    private static final long MAX_DAYS = 366;
    private static final int MAX_FUNNEL_STEPS = 8;
    private static final int MAX_FUNNEL_WINDOW_DAYS = 30;

    private final MixpanelClient mixpanelClient;
    private final SentryClient sentryClient;

    @GetMapping("/mixpanel")
    @Operation(summary = "Mixpanel 이벤트 일별 발생 수",
               description = "events 를 비우면 기간에 나타난 이벤트 전부를 센다. 이벤트·사용자·가입 집계를 함께 반환한다. 원본 이벤트를 받아 세므로 기간은 31일까지")
    public ApiResponse<MixpanelEventReport> mixpanel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) List<String> events) {
        validateRange(from, to, MixpanelClient.MAX_DAYS);
        return ApiResponse.onSuccess(mixpanelClient.fetchDailyCounts(events, from, to));
    }

    @GetMapping("/mixpanel/funnel")
    @Operation(summary = "Mixpanel 이벤트 퍼널 이탈율",
               description = "steps 순서대로 밟은 고유 사용자를 센다. 다음 단계는 앞 단계 이후여야 하고 첫 단계로부터 windowDays 안에 끝나야 한다")
    public ApiResponse<MixpanelFunnelReport> mixpanelFunnel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam List<String> steps,
            @RequestParam(defaultValue = "7") int windowDays) {
        validateRange(from, to, MixpanelClient.MAX_DAYS);
        validateSteps(steps);
        validateWindow(windowDays);
        return ApiResponse.onSuccess(mixpanelClient.fetchFunnel(steps, windowDays, from, to));
    }

    @GetMapping("/sentry")
    @Operation(summary = "Sentry 사용자 이벤트와 프로젝트 관측 데이터",
               description = "analytics_event 태그의 모든 사용자 이벤트와 오류·로그·성능·세션·릴리즈를 프로젝트별로 제공한다. 토큰·프로젝트 설정이 없으면 NOT_CONFIGURED")
    public ApiResponse<SentryIssueReport> sentry(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to, MAX_DAYS);
        return ApiResponse.onSuccess(sentryClient.fetchUnresolvedIssues(from, to));
    }

    private void validateSteps(List<String> steps) {
        if (steps == null || steps.size() < 2) {
            throw new IllegalArgumentException("퍼널은 단계가 2개 이상이어야 합니다.");
        }
        if (steps.size() > MAX_FUNNEL_STEPS) {
            throw new IllegalArgumentException("퍼널 단계는 " + MAX_FUNNEL_STEPS + "개까지입니다.");
        }
        if (steps.stream().anyMatch(step -> step == null || step.isBlank())) {
            throw new IllegalArgumentException("퍼널 단계 이름이 비어 있습니다.");
        }
    }

    private void validateWindow(int windowDays) {
        if (windowDays < 1 || windowDays > MAX_FUNNEL_WINDOW_DAYS) {
            throw new IllegalArgumentException(
                    "전환 윈도우는 1일부터 " + MAX_FUNNEL_WINDOW_DAYS + "일까지입니다.");
        }
    }

    private void validateRange(LocalDate from, LocalDate to, long maxDays) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days < 1 || days > maxDays) {
            throw new IllegalArgumentException("조회 기간은 1일부터 " + maxDays + "일까지입니다.");
        }
    }
}
