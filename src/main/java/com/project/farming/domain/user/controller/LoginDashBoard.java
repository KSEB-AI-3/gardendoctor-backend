package com.project.farming.domain.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginDashBoard {

    //TODO : test 용도이기에 운영단계에서 삭제 필요.(securityconfig 및 templetes 삭제)

    @GetMapping("/logindashboard")
    public String loginDashboard() {
        return "logindashboard"; // resources/templates/logindashboard.html 렌더링
    }
    @GetMapping("/login-success")
    public String loginSuccess() {
        return "login-success";  // templates/login-success.html을 렌더링
    }
}
