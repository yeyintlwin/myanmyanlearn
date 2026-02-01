package com.barlarlar.myanmyanlearn.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class SplashController {

    @GetMapping("/splash")
    public String splashPage(Model model) {
        log.info("SplashController.splashPage() called");
        return "splash";
    }
}
