package com.barlarlar.myanmyanlearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.repository.PasswordResetTokenRepository;
import com.barlarlar.myanmyanlearn.service.EmailService;
import com.barlarlar.myanmyanlearn.service.JwtService;
import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.entity.PasswordResetToken;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class ForgetPasswordController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

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

        System.out.println("=== Forget Password Request ===");
        System.out.println("Email: " + email);

        boolean exists = memberRepository.existsByEmail(email);
        System.out.println("Email exists: " + exists);

        if (!exists) {
            System.out.println("Email not found, returning to form");
            model.addAttribute("error", "Email not found. Please check and try again.");
            model.addAttribute("email", email);
            return "forget-password";
        }

        try {
            // Look up member and username for this email
            var memberOpt = memberRepository.findByEmail(email);
            if (memberOpt.isEmpty()) {
                System.out.println("Member not found for existing email, returning to form");
                model.addAttribute("error", "Email not found. Please check and try again.");
                model.addAttribute("email", email);
                return "forget-password";
            }
            Member member = memberOpt.get();
            String username = member.getUserId();

            // Build a display name using first and last name when available
            String firstName = member.getFirstName();
            String lastName = member.getLastName();
            String fullName;
            if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
                fullName = firstName + " " + lastName;
            } else if (firstName != null && !firstName.isBlank()) {
                fullName = firstName;
            } else if (lastName != null && !lastName.isBlank()) {
                fullName = lastName;
            } else {
                fullName = username;
            }

            // Generate JWT reset token
            System.out.println("Generating JWT token...");
            String token = jwtService.generatePasswordResetToken(email);
            System.out.println("Token generated: " + token.substring(0, 20) + "...");

            // Store token in database with 1 hour expiry
            Instant expireTime = Instant.now().plusSeconds(3600); // 1 hour
            System.out.println("Creating PasswordResetToken...");
            PasswordResetToken resetToken = new PasswordResetToken(token, email, expireTime);
            passwordResetTokenRepository.save(resetToken);
            System.out.println("Token saved to database");

            // Build reset link with JWT
            String resetLink = "/reset-password?token="
                    + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Reset link: " + resetLink);

            // Send email including full name (greeting) and username to help user identify
            // their account
            System.out.println("Sending email...");
            emailService.sendPasswordResetEmail(email, resetLink, username, fullName);
            System.out.println("Email sent successfully");

            System.out.println("Redirecting to reset-link-sent");
            return "redirect:/reset-link-sent";
        } catch (Exception e) {
            System.err.println("Error in forget password: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "An error occurred. Please try again.");
            model.addAttribute("email", email);
            return "forget-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        // Check if token exists in database
        var tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return "reset-link-expired";
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Check if token has expired
        if (resetToken.getExpireTime().isBefore(Instant.now())) {
            // Delete expired token
            passwordResetTokenRepository.delete(resetToken);
            return "reset-link-expired";
        }

        // Verify JWT signature and extract email
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
            RedirectAttributes redirectAttributes) {

        // Check if token exists in database
        var tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return "reset-link-expired";
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Check if token has expired
        if (resetToken.getExpireTime().isBefore(Instant.now())) {
            // Delete expired token
            passwordResetTokenRepository.delete(resetToken);
            return "reset-link-expired";
        }

        // Verify JWT signature and extract email
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

        // Delete the token from database after successful password reset
        passwordResetTokenRepository.delete(resetToken);

        return "redirect:/reset-success";
    }

    @GetMapping("/reset-link-sent")
    public String showResetLinkSentPage() {
        return "reset-link-sent";
    }

    @GetMapping("/reset-success")
    public String showResetSuccessPage() {
        return "reset-success";
    }
}
