package com.project.farming.global.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/admin")
@RequiredArgsConstructor
@Controller
public class WebController {

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    @GetMapping({"/", "/home"})
    public String home() {
        return "admin/index";
    }

    @GetMapping("/map")
    public String mapPage(Model model) {
        model.addAttribute("kakaoApiKey", kakaoApiKey);
        return "map/map";
    }
}