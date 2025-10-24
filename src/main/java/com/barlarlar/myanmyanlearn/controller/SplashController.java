package com.barlarlar.myanmyanlearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SplashController {

    @GetMapping("/splash")
    public String splashPage(Model model) {
        System.out.println("=== SplashController.splashPage() called ===");
        return "splash";
    }
}
