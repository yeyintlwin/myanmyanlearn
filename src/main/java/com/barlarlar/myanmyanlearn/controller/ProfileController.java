package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.repository.AssessmentScoreRecordRepository;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.repository.OtpVerificationRepository;
import com.barlarlar.myanmyanlearn.repository.PasswordResetTokenRepository;
import com.barlarlar.myanmyanlearn.repository.RoleRepository;
import com.barlarlar.myanmyanlearn.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

@Controller
public class ProfileController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AssessmentScoreRecordRepository assessmentScoreRecordRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 50;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{M}][\\p{L}\\p{M} '\\-]*[\\p{L}\\p{M}]$");

    @GetMapping("/profile")
    public String profilePage(Model model) {
        System.out.println("=== ProfileController.profilePage() called ===");

        // Get authenticated user information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authentication: " + (authentication != null ? authentication.getName() : "null"));
        System.out.println("Is authenticated: " + (authentication != null && authentication.isAuthenticated()));
        System.out.println(
                "Is anonymous: " + (authentication != null && authentication.getName().equals("anonymousUser")));

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            String username = authentication.getName();
            System.out.println("Username: " + username);
            model.addAttribute("username", username);
            model.addAttribute("userInitials", getInitials(username));

            // Fetch user's full data from database
            Optional<Member> memberOpt = memberRepository.findById(Objects.requireNonNull(username));
            System.out.println("Fetching user data for: " + username);
            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                System.out.println("User found: " + member.getFirstName() + " " + member.getLastName() + " - "
                        + member.getEmail());
                model.addAttribute("userFirstName", member.getFirstName());
                model.addAttribute("userLastName", member.getLastName());
                model.addAttribute("userEmail", member.getEmail());
                model.addAttribute("userFullName", getFullName(member.getFirstName(), member.getLastName()));
                model.addAttribute("userInitials", getInitialsFromName(member.getFirstName(), member.getLastName()));
                model.addAttribute("userProfileImage", member.getProfileImage());
                model.addAttribute("userEmailVerified", member.getEmailVerified());
                model.addAttribute("userActive", member.getActive());
            } else {
                System.out.println("User not found in database: " + username);
            }
        } else {
            System.out.println("User not authenticated or is anonymous");
        }

        return "profile";
    }

    /**
     * Get user initials from username
     */
    private String getInitials(String username) {
        if (username == null || username.isEmpty()) {
            return "U";
        }
        return username.substring(0, 1).toUpperCase();
    }

    /**
     * Get user initials from first and last name
     */
    private String getInitialsFromName(String firstName, String lastName) {
        String firstInitial = (firstName != null && !firstName.isEmpty()) ? firstName.substring(0, 1).toUpperCase()
                : "";
        String lastInitial = (lastName != null && !lastName.isEmpty()) ? lastName.substring(0, 1).toUpperCase() : "";
        return firstInitial + lastInitial;
    }

    /**
     * Get full name from first and last name
     */
    private String getFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "User";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    @DeleteMapping("/profile/delete")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAccount(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("=== ProfileController.deleteAccount() called ===");

        Map<String, Object> body = new HashMap<>();

        try {
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                    authentication.getName().equals("anonymousUser")) {
                body.put("success", false);
                body.put("message", msg("profile.deleteAccount.msg.unauth"));
                return ResponseEntity.status(401).body(body);
            }

            String username = authentication.getName();
            System.out.println("Deleting account for user: " + username);

            // Find the user in database
            Optional<Member> memberOpt = memberRepository.findById(Objects.requireNonNull(username));
            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                System.out.println("Found user: " + member.getFirstName() + " " + member.getLastName());

                String email = member.getEmail();

                assessmentScoreRecordRepository.deleteByUserId(username);
                roleRepository.deleteByUserId(username);
                otpVerificationRepository.deleteByUserId(username);
                if (email != null && !email.isBlank()) {
                    passwordResetTokenRepository.deleteByEmail(email);
                    otpVerificationRepository.deleteByEmail(email);
                }
                loginAttemptService.deleteAttemptsForUser(username);

                memberRepository.delete(member);
                System.out.println("User account deleted successfully");

                // Logout the user
                new SecurityContextLogoutHandler().logout(request, response, authentication);

                body.put("success", true);
                body.put("message", msg("profile.deleteAccount.msg.success"));
                return ResponseEntity.ok(body);
            } else {
                System.out.println("User not found: " + username);
                body.put("success", false);
                body.put("message", msg("profile.deleteAccount.msg.notFound"));
                return ResponseEntity.status(404).body(body);
            }
        } catch (Exception e) {
            System.err.println("Error deleting account: " + e.getMessage());
            e.printStackTrace();
            body.put("success", false);
            body.put("message", msg("profile.deleteAccount.msg.error"));
            return ResponseEntity.status(500).body(body);
        }
    }

    /**
     * Change user name
     */
    @PostMapping("/profile/change-name")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changeName(@RequestBody Map<String, String> request) {
        System.out.println("=== ProfileController.changeName() called ===");

        Map<String, Object> response = new HashMap<>();

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            if (username == null || username.equals("anonymousUser")) {
                response.put("success", false);
                response.put("message", msg("profile.changeName.msg.unauth"));
                return ResponseEntity.status(401).body(response);
            }

            String firstName = request.get("firstName") != null ? request.get("firstName").trim() : null;
            String lastName = request.get("lastName") != null ? request.get("lastName").trim() : null;

            if (firstName == null || lastName == null || firstName.isEmpty() || lastName.isEmpty()) {
                response.put("success", false);
                response.put("message", msg("profile.changeName.msg.required"));
                return ResponseEntity.badRequest().body(response);
            }

            if (firstName.length() < NAME_MIN_LENGTH || lastName.length() < NAME_MIN_LENGTH) {
                response.put("success", false);
                response.put("message", msg("profile.changeName.msg.minLength"));
                return ResponseEntity.badRequest().body(response);
            }

            if (firstName.length() > NAME_MAX_LENGTH || lastName.length() > NAME_MAX_LENGTH) {
                response.put("success", false);
                response.put("message", msg("profile.changeName.msg.maxLength"));
                return ResponseEntity.badRequest().body(response);
            }

            if (!NAME_PATTERN.matcher(firstName).matches() || !NAME_PATTERN.matcher(lastName).matches()) {
                response.put("success", false);
                response.put("message", msg("profile.changeName.msg.invalid"));
                return ResponseEntity.badRequest().body(response);
            }

            Optional<Member> memberOptional = memberRepository.findByUserIdAndActive(username, true);
            if (!memberOptional.isPresent()) {
                response.put("success", false);
                response.put("message", msg("profile.changeName.msg.notFound"));
                return ResponseEntity.status(404).body(response);
            }

            Member member = memberOptional.get();

            member.setFirstName(firstName);
            member.setLastName(lastName);
            memberRepository.save(member);

            System.out
                    .println("Name changed successfully for user: " + username + " to: " + firstName + " " + lastName);

            response.put("success", true);
            response.put("message", msg("profile.changeName.msg.success"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error changing name: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            String rootMessage = rootMessage(e);
            if (rootMessage != null && rootMessage.toLowerCase().contains("incorrect string value")) {
                response.put("message", msg("profile.changeName.msg.charset"));
            } else {
                response.put("message", msg("profile.changeName.msg.error"));
            }
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Change user password
     */
    @PostMapping("/profile/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> request) {
        System.out.println("=== ProfileController.changePassword() called ===");

        Map<String, Object> response = new HashMap<>();

        try {
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            if (username == null || username.equals("anonymousUser")) {
                response.put("success", false);
                response.put("message", msg("profile.changePassword.msg.unauth"));
                return ResponseEntity.status(401).body(response);
            }

            // Get request parameters
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            if (oldPassword == null || newPassword == null || oldPassword.trim().isEmpty()
                    || newPassword.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", msg("profile.changePassword.msg.required"));
                return ResponseEntity.badRequest().body(response);
            }

            // Validate new password length
            if (newPassword.length() < 8) {
                response.put("success", false);
                response.put("message", msg("profile.changePassword.msg.minLength"));
                return ResponseEntity.badRequest().body(response);
            }

            // Find user in database
            Optional<Member> memberOptional = memberRepository.findByUserIdAndActive(username, true);
            if (!memberOptional.isPresent()) {
                response.put("success", false);
                response.put("message", msg("profile.changePassword.msg.notFound"));
                return ResponseEntity.status(404).body(response);
            }

            Member member = memberOptional.get();

            // Verify old password
            if (!passwordEncoder.matches(oldPassword, member.getPassword())) {
                response.put("success", false);
                response.put("message", msg("profile.changePassword.msg.incorrect"));
                return ResponseEntity.badRequest().body(response);
            }

            // Check if new password is different from old password
            if (passwordEncoder.matches(newPassword, member.getPassword())) {
                response.put("success", false);
                response.put("message", msg("profile.changePassword.msg.same"));
                return ResponseEntity.badRequest().body(response);
            }

            // Update password
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            member.setPassword(encodedNewPassword);
            memberRepository.save(member);

            System.out.println("Password changed successfully for user: " + username);

            response.put("success", true);
            response.put("message", msg("profile.changePassword.msg.success"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error changing password: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", msg("profile.changePassword.msg.error"));
            return ResponseEntity.status(500).body(response);
        }
    }

    private String msg(String code) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, null, code, locale);
    }

    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }
}
