package com.swyp.picke.domain.admin.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Hidden // 스웨거 노출 차단
@Controller
@RequestMapping("/api/v1/admin")
public class AdminPickeController {

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${picke.baseUrl}")
    private String baseUrl;

    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/api/v1/admin/login";
    }

    @GetMapping("/login")
    public String adminLoginPage(Model model) {
        model.addAttribute("kakaoClientId", kakaoClientId);
        model.addAttribute("googleClientId", googleClientId);
        model.addAttribute("redirectUri", baseUrl + "/api/v1/admin/login");

        return "admin/admin-login";
    }

    @GetMapping("/picke/list")
    public String pickeListPage() {
        return "admin/picke-list";
    }

    @GetMapping("/picke")
    public String pickeCreatePage() {
        return "admin/picke-create";
    }

    @GetMapping("/picke/notice")
    public String noticePage() {
        return "admin/admin-notice";
    }
}