package com.barlarlar.myanmyanlearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AppIntroController {

    @GetMapping("/intro")
    public String appIntroPage(Model model) {
        System.out.println("=== AppIntroController.appIntroPage() called ===");
        return "app-intro";
    }

    @PostMapping("/intro")
    @ResponseBody
    public String handleIntroCompletion(@RequestParam String action, HttpServletResponse response) {
        System.out.println("=== AppIntroController.handleIntroCompletion() called ===");
        System.out.println("Intro action: " + action);

        // Set intro completion status in cookie
        Cookie introCookie = new Cookie("introCompleted", action);
        introCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        introCookie.setPath("/");
        response.addCookie(introCookie);

        return "success";
    }
}
