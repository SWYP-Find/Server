package com.swyp.picke.domain.ad.controller;

import com.swyp.picke.domain.ad.service.AdQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * ad.picke.store 루트 공개 지면.
 * 쿠팡 파트너스 매체 심사에서 URL 접속 확인을 하므로 실제 콘텐츠가 있어야 한다.
 * 광고 도메인이 아닌 Host로 들어오면 최소 응답만 준다. API 도메인 루트에 광고 페이지가 뜨면 안 된다.
 */
@Controller
@RequiredArgsConstructor
public class AdLandingController {

    private final AdQueryService adQueryService;

    @Value("${picke.ad.host:ad.picke.store}")
    private String adHost;

    @GetMapping("/")
    public Object landing(HttpServletRequest request, Model model) {
        if (!adHost.equalsIgnoreCase(request.getServerName())) {
            return ResponseEntity.ok("PICKE");
        }

        model.addAttribute("ads", adQueryService.findLandingAds());
        return "ad/landing";
    }
}
