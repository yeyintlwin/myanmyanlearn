package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CourseService courseService;

    @GetMapping("/home")
    public String homePage(Model model) {
        System.out.println("=== HomeController.homePage() called ===");

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
            Optional<Member> memberOpt = memberRepository.findById(username);
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
            } else {
                System.out.println("User not found in database: " + username);
            }
        } else {
            System.out.println("User not authenticated or is anonymous");
        }

        // Add courses from JSON data model
        model.addAttribute("courses", courseService.getAllCourses());

        return "home";
    }

    /**
     * Get user initials from username
     */
    private String getInitials(String username) {
        if (username == null || username.isEmpty()) {
            return "U";
        }

        // If username has multiple words, take first letter of each word
        String[] words = username.split("\\s+");
        if (words.length >= 2) {
            return (words[0].charAt(0) + "" + words[1].charAt(0)).toUpperCase();
        }

        // If single word, take first two characters
        if (username.length() >= 2) {
            return username.substring(0, 2).toUpperCase();
        }

        // If single character, use it twice
        return (username.charAt(0) + "" + username.charAt(0)).toUpperCase();
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

    /**
     * Get initials from first and last name
     */
    private String getInitialsFromName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "U";
        }
        if (firstName == null) {
            return lastName.substring(0, 1).toUpperCase();
        }
        if (lastName == null) {
            return firstName.substring(0, 1).toUpperCase();
        }
        return (firstName.substring(0, 1) + lastName.substring(0, 1)).toUpperCase();
    }
}
