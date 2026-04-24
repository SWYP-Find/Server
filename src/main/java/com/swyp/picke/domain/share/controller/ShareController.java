package com.swyp.picke.domain.share.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class ShareController {

    @Value("${picke.store.android:https://play.google.com/store/apps/details?id=com.picke.app}")
    private String androidStoreUrl;

    @GetMapping("/report/{reportId}")
    public Object report(@PathVariable Long reportId, HttpServletRequest request, Model model) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) ua = "";
        ua = ua.toLowerCase();

        if (ua.contains("android")) {
            return new RedirectView(androidStoreUrl);
        } else {
            model.addAttribute("reportId", reportId);
            return "share/report";
        }
    }

    @GetMapping("/battle/{battleId}")
    public Object battle(@PathVariable Long battleId, HttpServletRequest request, Model model) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) ua = "";
        ua = ua.toLowerCase();

        if (ua.contains("android")) {
            return new RedirectView(androidStoreUrl);
        } else {
            model.addAttribute("battleId", battleId);
            return "share/battle";
        }
    }
}
