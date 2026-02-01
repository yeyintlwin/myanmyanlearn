package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Random;

@Controller
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    @GetMapping("/test-email")
    @ResponseBody
    public String testEmail(@RequestParam String email) {
        try {
            emailService.sendTestEmail(email);
            return "Test email sent successfully to: " + email;
        } catch (Exception e) {
            return "Failed to send test email: " + e.getMessage();
        }
    }

    @GetMapping("/test-otp")
    @ResponseBody
    public String testOtpEmail(@RequestParam String email) {
        try {
            // Generate a random 6-digit OTP
            String otpCode = String.format("%06d", new Random().nextInt(1000000));
            emailService.sendOtpEmail(email, otpCode);
            return "OTP email sent successfully to: " + email + " with code: " + otpCode;
        } catch (Exception e) {
            return "Failed to send OTP email: " + e.getMessage();
        }
    }
}
