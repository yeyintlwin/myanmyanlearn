package com.barlarlar.myanmyanlearn.security;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.service.EmailService;
import com.barlarlar.myanmyanlearn.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpService otpService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String username = authentication.getName();
        System.out.println("Login attempt for user: " + username);

        // Get user from database
        Member member = memberRepository.findById(username).orElse(null);

        if (member != null) {
            System.out.println("User found: " + username + ", Email verified: " + member.getEmailVerified());

            // Check if email is verified
            if (!member.getEmailVerified()) {
                System.out.println("User email not verified, clearing session and redirecting to email verification");

                // Clear any existing session for unverified users - NO SESSION STORAGE
                request.getSession().invalidate();

                try {
                    System.out.println("Attempting to send OTP to: " + member.getEmail());

                    // Generate new OTP
                    String otpCode = otpService.generateOtp();
                    java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusMinutes(10);

                    // Update member with new OTP
                    member.setOtpCode(otpCode);
                    member.setOtpExpiresAt(expiresAt);
                    memberRepository.save(member);

                    // Send OTP email
                    emailService.sendOtpEmail(member.getEmail(), otpCode);

                    System.out.println("New OTP sent to: " + member.getEmail());
                } catch (Exception e) {
                    System.err.println("Failed to send OTP to " + member.getEmail() + ": " + e.getMessage());
                    e.printStackTrace();
                    // Continue with redirect even if OTP sending fails
                }

                // Redirect to email verification page with email parameter and success message
                // NO SESSION STORAGE for unverified users
                response.sendRedirect(
                        "/email-verification?email=" + member.getEmail() + "&forceVerification=true&otpSent=true");
                return;
            }
        }

        // If email is verified or user not found, proceed to home page
        System.out.println("User email verified, redirecting to home");
        response.sendRedirect("/home");
    }
}
