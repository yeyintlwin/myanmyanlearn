package com.barlarlar.myanmyanlearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String splash() {
        return "home";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }
    /*
     * @GetMapping("/intro")
     * public String appIntro() {
     * return "app-intro";
     * }
     * 
     * @GetMapping("/forget-password")
     * public String forgetPassword() {
     * return "forget-password";
     * }
     * 
     * @GetMapping("/reset-password")
     * public String resetPassword() {
     * return "reset-password";
     * }
     * 
     * @GetMapping("/reset-success")
     * public String resetSuccess() {
     * return "reset-success";
     * }
     */
}
