package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Slf4j
@RequiredArgsConstructor
public class SignUpController {

    private final RegistrationService registrationService;

    @GetMapping("/register")
    public String register(Model model) {
        // Initialize empty values to prevent template errors
        model.addAttribute("username", "");
        model.addAttribute("firstName", "");
        model.addAttribute("lastName", "");
        model.addAttribute("email", "");

        log.debug("GET /register - Model attributes: username={}, firstName={}, lastName={}, email={}",
                model.getAttribute("username"),
                model.getAttribute("firstName"),
                model.getAttribute("lastName"),
                model.getAttribute("email"));
        return "register";
    }

    @GetMapping("/register-test")
    public String registerTest(Model model) {
        // Test with some values to see if Thymeleaf is working
        model.addAttribute("username", "testuser");
        model.addAttribute("firstName", "Test");
        model.addAttribute("lastName", "User");
        model.addAttribute("email", "test@example.com");

        log.debug("GET /register-test - Model attributes: username={}, firstName={}, lastName={}, email={}",
                model.getAttribute("username"),
                model.getAttribute("firstName"),
                model.getAttribute("lastName"),
                model.getAttribute("email"));
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("Registration attempt for user: {}, email: {}", username, email);

        try {
            // Register the user
            Member member = registrationService.registerUser(username, password, firstName, lastName, email);
            log.info("User registered successfully: {}", member.getUserId());

            // Redirect to email verification with user info
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Please check your email for verification code.");

            return "redirect:/email-verification";

        } catch (Exception e) {
            log.error("Registration failed for user: {}", username, e);

            // Add error message to model
            model.addAttribute("error", e.getMessage());

            // Preserve form data for better user experience
            model.addAttribute("username", username);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);

            log.debug("Model attributes set - username={}, firstName={}, lastName={}, email={}",
                    username, firstName, lastName, email);
            return "register";
        }
    }

    @GetMapping("/email-verification")
    public String emailVerification(@RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "forceVerification", required = false) String forceVerification,
            @RequestParam(value = "otpSent", required = false) String otpSent,
            Model model) {
        // Get email from URL parameter or flash attributes
        if (email != null) {
            model.addAttribute("email", email);
        }
        if (forceVerification != null) {
            model.addAttribute("forceVerification", true);
        }
        if (otpSent != null) {
            model.addAttribute("success", "A new verification code has been sent to your email address.");
        }
        return "email-verification";
    }

    @PostMapping("/email-verification")
    public String processEmailVerification(
            @RequestParam("email") String email,
            @RequestParam("otpCode") String otpCode,
            RedirectAttributes redirectAttributes) {

        log.info("Email verification attempt - Email: {}", email);

        try {
            // Verify the OTP
            boolean verified = registrationService.verifyEmail(email, otpCode);
            log.info("OTP verification result: {}", verified);

            if (verified) {
                redirectAttributes.addFlashAttribute("success", "Email verified successfully! You can now login.");
                log.info("Redirecting to verification-success");
                return "redirect:/verification-success";
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid or expired OTP code. Please try again.");
                redirectAttributes.addFlashAttribute("email", email); // Preserve email for retry
                log.info("Invalid OTP, staying on email-verification");
                return "redirect:/email-verification";
            }

        } catch (Exception e) {
            log.error("Error during OTP verification for email: {}", email, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("email", email); // Preserve email for retry
            return "redirect:/email-verification";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        try {
            registrationService.resendOtp(email);
            redirectAttributes.addFlashAttribute("success", "New OTP code sent to your email.");
            redirectAttributes.addFlashAttribute("email", email); // Preserve email
            return "redirect:/email-verification";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("email", email); // Preserve email even on error
            return "redirect:/email-verification";
        }
    }

    @GetMapping("/verification-success")
    public String verificationSuccess() {
        return "verification-success";
    }

}
