package com.barlarlar.myanmyanlearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.beans.factory.annotation.Autowired;

import com.barlarlar.myanmyanlearn.service.LoginAttemptService;
import com.barlarlar.myanmyanlearn.service.RegistrationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private RegistrationService registrationService;

    @GetMapping("/login")
    public String showMyLoginPage(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "locked", required = false) String locked,
            @RequestParam(value = "disabled", required = false) String disabled,
            @RequestParam(value = "expired", required = false) String expired,
            Model model) {

        if (error != null) {
            model.addAttribute("error", true);
        }
        if (logout != null) {
            model.addAttribute("logout", true);
        }
        if (locked != null) {
            model.addAttribute("locked", true);
        }
        if (disabled != null) {
            model.addAttribute("disabled", true);
        }
        if (expired != null) {
            model.addAttribute("expired", true);
        }

        return "login";
    }

    @PostMapping("/authenticateTheUser")
    public String authenticateUser(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String ipAddress = getClientIpAddress(request);
        String username = request.getParameter("username");

        // Check rate limiting
        if (loginAttemptService.isIpRateLimited(ipAddress)) {
            int remainingMinutes = loginAttemptService.getIpRateLimitRemainingMinutes(ipAddress);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Too many login attempts from this IP. Please try again in " + remainingMinutes + " minutes.");
            return "redirect:/login?error=true";
        }

        // Check if user is locked
        if (username != null && loginAttemptService.isUserLocked(username)) {
            int remainingMinutes = loginAttemptService.getUserLockoutRemainingMinutes(username);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your account has been temporarily locked due to too many failed attempts. Please try again in "
                            + remainingMinutes + " minutes.");
            redirectAttributes.addFlashAttribute("locked", true);
            return "redirect:/login?error=true";
        }

        HttpSession session = request.getSession(false);

        if (session != null) {
            AuthenticationException ex = (AuthenticationException) session
                    .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

            if (ex != null) {
                String errorMessage = "Invalid username or password. Please try again.";

                // Record failed attempt
                loginAttemptService.recordFailedAttempt(ipAddress, username);

                if (ex instanceof BadCredentialsException) {
                    errorMessage = "Invalid username or password. Please check your credentials and try again.";
                } else if (ex instanceof LockedException) {
                    errorMessage = "Your account has been locked. Please contact support.";
                    redirectAttributes.addFlashAttribute("locked", true);
                } else if (ex instanceof DisabledException) {
                    errorMessage = "Your account has been disabled. Please contact support.";
                    redirectAttributes.addFlashAttribute("disabled", true);
                } else if (ex instanceof AccountExpiredException) {
                    errorMessage = "Your account has expired. Please contact support.";
                    redirectAttributes.addFlashAttribute("expired", true);
                } else if (ex instanceof CredentialsExpiredException) {
                    errorMessage = "Your password has expired. Please reset your password.";
                    redirectAttributes.addFlashAttribute("expired", true);
                }

                redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            } else {
                // Successful authentication - check email verification
                try {
                    // Check if user's email is verified
                    if (username != null && !registrationService.isEmailVerified(username)) {
                        // User is authenticated but email not verified - clear session and force email
                        // verification
                        request.getSession().invalidate(); // Clear session for unverified users
                        redirectAttributes.addFlashAttribute("errorMessage",
                                "Please verify your email address before logging in. Check your email for the verification code.");
                        redirectAttributes.addFlashAttribute("email", getEmailForUser(username));
                        redirectAttributes.addFlashAttribute("forceVerification", true);
                        return "redirect:/email-verification";
                    }

                    // Email is verified - record successful login
                    loginAttemptService.recordSuccessfulAttempt(ipAddress, username);
                } catch (Exception e) {
                    // If we can't check email verification, allow login but log the issue
                    System.err.println(
                            "Could not check email verification for user: " + username + " - " + e.getMessage());
                    loginAttemptService.recordSuccessfulAttempt(ipAddress, username);
                }
            }
        }

        return "redirect:/login?error=true";
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Get email address for a user
     */
    private String getEmailForUser(String username) {
        try {
            return registrationService.getUserByUsername(username).getEmail();
        } catch (Exception e) {
            System.err.println("Could not get email for user: " + username + " - " + e.getMessage());
            return username; // Fallback to username
        }
    }
}
