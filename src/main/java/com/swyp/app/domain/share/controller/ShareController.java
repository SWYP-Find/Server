package com.swyp.app.domain.share.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ShareController {

    @GetMapping("/result/{userId}")
    public String result(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "share/result";
    }
}
