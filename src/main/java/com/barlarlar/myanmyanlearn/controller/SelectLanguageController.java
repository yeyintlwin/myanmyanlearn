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
public class SelectLanguageController {

    @GetMapping("/language")
    public String selectLanguagePage(Model model) {
        System.out.println("=== SelectLanguageController.selectLanguagePage() called ===");
        return "language";
    }

    @PostMapping("/language")
    @ResponseBody
    public String handleLanguageSelection(@RequestParam String language, HttpServletResponse response) {
        System.out.println("=== SelectLanguageController.handleLanguageSelection() called ===");
        System.out.println("Selected language: " + language);

        // Set language preference in cookie
        Cookie languageCookie = new Cookie("selectedLanguage", language);
        languageCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        languageCookie.setPath("/");
        response.addCookie(languageCookie);

        return "success";
    }
}
