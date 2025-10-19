package com.barlarlar.myanmyanlearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SignUpController {

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/email-verification")
    public String emailVerification() {
        return "email-verification";
    }

    @GetMapping("/verification-success")
    public String verificationSuccess() {
        return "verification-success";
    }
}
