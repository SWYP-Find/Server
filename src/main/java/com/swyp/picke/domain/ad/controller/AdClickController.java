package com.swyp.picke.domain.ad.controller;

import com.swyp.picke.domain.ad.service.AdClickService;
import com.swyp.picke.domain.ad.service.AdClickService.AdClickTarget;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 제휴 링크 클릭 진입점.
 * /api/v1 아래에 두지 않는다. 외부 브라우저가 여는 공개 숏링크라 짧아야 하고, JSON API가 아니다.
 */
@Controller
@RequiredArgsConstructor
public class AdClickController {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final AdClickService adClickService;

    @Value("${picke.ad.base-url:https://ad.picke.store}")
    private String adBaseUrl;

    @GetMapping("/c/{code}")
    public RedirectView click(@PathVariable String code, HttpServletRequest request) {
        Optional<AdClickTarget> target = adClickService.resolveTarget(code);

        if (target.isEmpty()) {
            // 만료되었거나 없는 코드다. 404를 보여주는 대신 랜딩으로 흘려보낸다.
            return new RedirectView(adBaseUrl + "/");
        }

        AdClickTarget clickTarget = target.get();
        adClickService.recordClick(clickTarget, resolveClientIp(request), request.getHeader("User-Agent"));

        return new RedirectView(clickTarget.redirectUrl());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR);
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwardedFor.split(",")[0].trim();
    }
}
