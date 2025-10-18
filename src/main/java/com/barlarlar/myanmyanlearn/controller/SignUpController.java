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

    @GetMapping("/forget-password")
    public String forgetPassword() {
        return "forget-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    @GetMapping("/reset-success")
    public String resetSuccess() {
        return "reset-success";
    }

    @GetMapping("/verification-success")
    public String verificationSuccess() {
        return "verification-success";
    }
}
