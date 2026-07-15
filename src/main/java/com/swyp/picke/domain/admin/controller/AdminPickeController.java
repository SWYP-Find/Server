package com.swyp.picke.domain.admin.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 관리자 페이지는 별도 Next.js 앱(admin-web)으로 이관되었다.
 * 기존 admin 경로(picke.store/api/v1/admin/**)는 새 앱으로 리다이렉트한다.
 * OAuth 콜백(?code=...)은 쿼리스트링을 보존해 새 앱 로그인 페이지로 전달한다.
 */
@Hidden
@Controller
@RequestMapping("/api/v1/admin")
public class AdminPickeController {

    @Value("${admin.web.url:https://picke.up.railway.app}")
    private String adminWebUrl;

    private String redirect(String path, HttpServletRequest request) {
        String qs = request.getQueryString();
        return "redirect:" + adminWebUrl + path + (qs != null ? "?" + qs : "");
    }

    @GetMapping({"", "/"})
    public String index(HttpServletRequest request) {
        return redirect("/login", request);
    }

    @GetMapping("/login")
    public String adminLoginPage(HttpServletRequest request) {
        return redirect("/login", request);
    }

    @GetMapping("/picke/list")
    public String pickeListPage(HttpServletRequest request) {
        return redirect("/picke/list", request);
    }

    @GetMapping("/picke")
    public String pickeCreatePage(HttpServletRequest request) {
        return redirect("/picke/create", request);
    }

    @GetMapping("/picke/notice")
    public String noticePage(HttpServletRequest request) {
        return redirect("/notice", request);
    }
}
