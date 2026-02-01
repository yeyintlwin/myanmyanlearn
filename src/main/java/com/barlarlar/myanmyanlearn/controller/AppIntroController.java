package com.barlarlar.myanmyanlearn.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@Slf4j
public class AppIntroController {

    @GetMapping("/intro")
    public String appIntroPage(Model model) {
        log.info("AppIntroController.appIntroPage() called");
        return "app-intro";
    }

    @PostMapping("/intro")
    @ResponseBody
    public String handleIntroCompletion(@RequestParam String action, HttpServletResponse response) {
        log.info("AppIntroController.handleIntroCompletion() called. action={}", action);

        // Set intro completion status in cookie
        Cookie introCookie = new Cookie("introCompleted", action);
        introCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        introCookie.setPath("/");
        response.addCookie(introCookie);

        return "success";
    }
}
