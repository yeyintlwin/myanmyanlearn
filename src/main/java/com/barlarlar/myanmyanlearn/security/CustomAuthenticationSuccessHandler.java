package com.barlarlar.myanmyanlearn.security;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.service.EmailService;
import com.barlarlar.myanmyanlearn.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final MemberRepository memberRepository;
    private final EmailService emailService;
    private final OtpService otpService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String username = authentication.getName();
        log.info("Login attempt for user: {}", username);

        // Get user from database
        Member member = memberRepository.findById(Objects.requireNonNull(username)).orElse(null);

        if (member != null) {
            log.debug("User found: {}, Email verified: {}", username, member.getEmailVerified());

            // Check if email is verified
            if (!member.getEmailVerified()) {
                log.info("User email not verified, redirecting to email verification");

                // Clear any existing session for unverified users - NO SESSION STORAGE
                request.getSession().invalidate();

                try {
                    log.info("Attempting to send OTP to: {}", member.getEmail());

                    // Generate new OTP
                    String otpCode = otpService.generateOtp();
                    java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusMinutes(10);

                    // Update member with new OTP
                    member.setOtpCode(otpCode);
                    member.setOtpExpiresAt(expiresAt);
                    memberRepository.save(member);

                    // Send OTP email
                    emailService.sendOtpEmail(member.getEmail(), otpCode);

                    log.info("New OTP sent to: {}", member.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send OTP to {}", member.getEmail(), e);
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
        // If the remember-me checkbox was NOT checked, clear any existing remember-me
        // cookie
        if (request.getParameter("remember-me") == null) {
            Cookie rememberMeCookie = new Cookie("remember-me", null);
            rememberMeCookie.setPath("/");
            rememberMeCookie.setMaxAge(0);
            response.addCookie(rememberMeCookie);
        }
        log.info("User email verified, redirecting to home");
        response.sendRedirect("/home");
    }
}
