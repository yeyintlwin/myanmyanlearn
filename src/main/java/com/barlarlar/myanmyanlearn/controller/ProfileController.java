package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

@Controller
public class ProfileController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    public ResponseEntity<String> deleteAccount(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("=== ProfileController.deleteAccount() called ===");

        try {
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                    authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(401).body("Not authenticated");
            }

            String username = authentication.getName();
            System.out.println("Deleting account for user: " + username);

            // Find the user in database
            Optional<Member> memberOpt = memberRepository.findById(Objects.requireNonNull(username));
            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                System.out.println("Found user: " + member.getFirstName() + " " + member.getLastName());

                // Delete the user from database
                memberRepository.delete(member);
                System.out.println("User account deleted successfully");

                // Logout the user
                new SecurityContextLogoutHandler().logout(request, response, authentication);

                return ResponseEntity.ok("Account deleted successfully");
            } else {
                System.out.println("User not found: " + username);
                return ResponseEntity.status(404).body("User not found");
            }
        } catch (Exception e) {
            System.err.println("Error deleting account: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error deleting account");
        }
    }

    /**
     * Change user email
     */
    @PostMapping("/profile/change-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changeEmail(@RequestBody Map<String, String> request) {
        System.out.println("=== ProfileController.changeEmail() called ===");

        Map<String, Object> response = new HashMap<>();

        try {
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            if (username == null || username.equals("anonymousUser")) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.status(401).body(response);
            }

            // Get request parameters
            String newEmail = request.get("newEmail");

            if (newEmail == null || newEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "New email address is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Basic email validation
            if (!newEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                response.put("success", false);
                response.put("message", "Please enter a valid email address");
                return ResponseEntity.badRequest().body(response);
            }

            // Find user in database
            Optional<Member> memberOptional = memberRepository.findByUserIdAndActive(username, true);
            if (!memberOptional.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(404).body(response);
            }

            Member member = memberOptional.get();

            // Check if email is already in use by another user
            Optional<Member> existingEmailMember = memberRepository.findByEmail(newEmail.trim());
            if (existingEmailMember.isPresent() && !existingEmailMember.get().getUserId().equals(username)) {
                response.put("success", false);
                response.put("message", "This email address is already in use by another account");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if it's the same email
            if (member.getEmail().equals(newEmail.trim())) {
                response.put("success", false);
                response.put("message", "New email must be different from current email");
                return ResponseEntity.badRequest().body(response);
            }

            // Update email and set as unverified
            member.setEmail(newEmail.trim());
            member.setEmailVerified(false);
            memberRepository.save(member);

            System.out.println("Email change request processed for user: " + username + " to: " + newEmail);

            response.put("success", true);
            response.put("message", "Email change request processed. Please check your new email for verification.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error changing email: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "An error occurred while updating email");
            return ResponseEntity.status(500).body(response);
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
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            if (username == null || username.equals("anonymousUser")) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.status(401).body(response);
            }

            // Get request parameters
            String firstName = request.get("firstName");
            String lastName = request.get("lastName");

            if (firstName == null || lastName == null || firstName.trim().isEmpty() || lastName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Both first name and last name are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate name length
            if (firstName.trim().length() < 2 || lastName.trim().length() < 2) {
                response.put("success", false);
                response.put("message", "Names must be at least 2 characters long");
                return ResponseEntity.badRequest().body(response);
            }

            // Find user in database
            Optional<Member> memberOptional = memberRepository.findByUserIdAndActive(username, true);
            if (!memberOptional.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(404).body(response);
            }

            Member member = memberOptional.get();

            // Update names
            member.setFirstName(firstName.trim());
            member.setLastName(lastName.trim());
            memberRepository.save(member);

            System.out
                    .println("Name changed successfully for user: " + username + " to: " + firstName + " " + lastName);

            response.put("success", true);
            response.put("message", "Name updated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error changing name: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "An error occurred while updating name");
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
                response.put("message", "User not authenticated");
                return ResponseEntity.status(401).body(response);
            }

            // Get request parameters
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            if (oldPassword == null || newPassword == null || oldPassword.trim().isEmpty()
                    || newPassword.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Old password and new password are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate new password length
            if (newPassword.length() < 8) {
                response.put("success", false);
                response.put("message", "New password must be at least 8 characters long");
                return ResponseEntity.badRequest().body(response);
            }

            // Find user in database
            Optional<Member> memberOptional = memberRepository.findByUserIdAndActive(username, true);
            if (!memberOptional.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(404).body(response);
            }

            Member member = memberOptional.get();

            // Verify old password
            if (!passwordEncoder.matches(oldPassword, member.getPassword())) {
                response.put("success", false);
                response.put("message", "Current password is incorrect");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if new password is different from old password
            if (passwordEncoder.matches(newPassword, member.getPassword())) {
                response.put("success", false);
                response.put("message", "New password must be different from current password");
                return ResponseEntity.badRequest().body(response);
            }

            // Update password
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            member.setPassword(encodedNewPassword);
            memberRepository.save(member);

            System.out.println("Password changed successfully for user: " + username);

            response.put("success", true);
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error changing password: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "An error occurred while changing password");
            return ResponseEntity.status(500).body(response);
        }
    }
}
