package com.barlarlar.myanmyanlearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.service.EmailService;
import com.barlarlar.myanmyanlearn.service.JwtService;
import com.barlarlar.myanmyanlearn.entity.Member;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class ForgetPasswordController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/forget-password")
    public String showForgetPasswordPage(Model model) {
        return "forget-password";
    }

    @PostMapping("/forget-password")
    public String handleForgetPassword(@RequestParam("email") String email,
            Model model,
            RedirectAttributes redirectAttributes) {

        boolean exists = memberRepository.existsByEmail(email);
        if (!exists) {
            model.addAttribute("error", "Email not found. Please check and try again.");
            model.addAttribute("email", email);
            return "forget-password";
        }

        // Generate JWT reset token (self-contained)
        String token = jwtService.generatePasswordResetToken(email);

        // Build reset link with JWT
        String resetLink = "/reset-password?token="
                + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);

        // Send email
        emailService.sendPasswordResetEmail(email, resetLink);

        return "redirect:/reset-link-sent";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        // With JWT, verify signature/expiry and extract email
        String emailFromToken;
        try {
            emailFromToken = jwtService.parsePasswordResetEmail(token);
        } catch (Exception ex) {
            return "reset-link-expired";
        }
        if (emailFromToken == null) {
            return "reset-link-expired";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("token") String token,
            @RequestParam("password") String newPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        String emailFromToken;
        try {
            emailFromToken = jwtService.parsePasswordResetEmail(token);
        } catch (Exception ex) {
            return "reset-link-expired";
        }
        if (emailFromToken == null) {
            return "reset-link-expired";
        }
        String email = emailFromToken;

        var memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isEmpty()) {
            return "reset-link-expired";
        }

        Member member = memberOpt.get();
        String hashed = passwordEncoder.encode(newPassword);
        member.setPassword(hashed);
        memberRepository.save(member);

        return "redirect:/reset-success";
    }

    @GetMapping("/reset-link-sent")
    public String showResetLinkSentPage() {
        return "reset-link-sent";
    }
}
