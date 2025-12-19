package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SignUpController {

    @Autowired
    private RegistrationService registrationService;

    @GetMapping("/register")
    public String register(Model model) {
        // Initialize empty values to prevent template errors
        model.addAttribute("username", "");
        model.addAttribute("firstName", "");
        model.addAttribute("lastName", "");
        model.addAttribute("email", "");

        System.out.println("GET /register - Model attributes: username=" + model.getAttribute("username") +
                ", firstName=" + model.getAttribute("firstName") +
                ", lastName=" + model.getAttribute("lastName") +
                ", email=" + model.getAttribute("email"));
        return "register";
    }

    @GetMapping("/register-test")
    public String registerTest(Model model) {
        // Test with some values to see if Thymeleaf is working
        model.addAttribute("username", "testuser");
        model.addAttribute("firstName", "Test");
        model.addAttribute("lastName", "User");
        model.addAttribute("email", "test@example.com");

        System.out.println("GET /register-test - Model attributes: username=" + model.getAttribute("username") +
                ", firstName=" + model.getAttribute("firstName") +
                ", lastName=" + model.getAttribute("lastName") +
                ", email=" + model.getAttribute("email"));
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

        System.out.println("Registration attempt for user: " + username + ", email: " + email);

        try {
            // Register the user
            Member member = registrationService.registerUser(username, password, firstName, lastName, email);
            System.out.println("User registered successfully: " + member.getUserId());

            // Redirect to email verification with user info
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Please check your email for verification code.");

            return "redirect:/email-verification";

        } catch (Exception e) {
            System.err.println("Registration failed: " + e.getMessage());
            e.printStackTrace();

            // Add error message to model
            model.addAttribute("error", e.getMessage());

            // Preserve form data for better user experience
            model.addAttribute("username", username);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);

            System.out.println("Model attributes set - username: " + username + ", firstName: " + firstName
                    + ", lastName: " + lastName + ", email: " + email);
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

        System.out.println("Email verification attempt - Email: " + email + ", OTP: " + otpCode);

        try {
            // Verify the OTP
            boolean verified = registrationService.verifyEmail(email, otpCode);
            System.out.println("OTP verification result: " + verified);

            if (verified) {
                redirectAttributes.addFlashAttribute("success", "Email verified successfully! You can now login.");
                System.out.println("Redirecting to verification-success");
                return "redirect:/verification-success";
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid or expired OTP code. Please try again.");
                redirectAttributes.addFlashAttribute("email", email); // Preserve email for retry
                System.out.println("Invalid OTP, staying on email-verification");
                return "redirect:/email-verification";
            }

        } catch (Exception e) {
            System.err.println("Error during OTP verification: " + e.getMessage());
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
