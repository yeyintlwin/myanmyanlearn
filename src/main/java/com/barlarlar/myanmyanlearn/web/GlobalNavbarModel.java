package com.barlarlar.myanmyanlearn.web;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@ControllerAdvice
public class GlobalNavbarModel {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MessageSource messageSource;

    @ModelAttribute
    public void addGlobalUser(Model model) {
        if (model.containsAttribute("userFullName") && model.containsAttribute("username")
                && model.containsAttribute("userInitials") && model.containsAttribute("userEmail")
                && model.containsAttribute("userRoleLabel")) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;

        if (!model.containsAttribute("userRoleLabel")) {
            String roleKey = resolveRoleKey(auth);
            String label = messageSource.getMessage(roleKey, null, LocaleContextHolder.getLocale());
            model.addAttribute("userRoleLabel", label);
        }

        String displayName = null;
        String username = null;
        if (auth != null && auth.isAuthenticated() && !("anonymousUser".equals(auth.getPrincipal()))) {
            username = auth.getName();
            displayName = username; // Fallback until a full name source is available
        }

        // Try to enrich with Member info if available
        if (username != null) {
            try {
                Optional<Member> memberOpt = memberRepository != null ? memberRepository.findById(username) : Optional.empty();
                if (memberOpt.isPresent()) {
                    Member m = memberOpt.get();
                    String fullName = getFullName(m.getFirstName(), m.getLastName());
                    String nameInitials = computeInitialsFromName(m.getFirstName(), m.getLastName());

                    if (!model.containsAttribute("userFirstName") && m.getFirstName() != null) {
                        model.addAttribute("userFirstName", m.getFirstName());
                    }
                    if (!model.containsAttribute("userLastName") && m.getLastName() != null) {
                        model.addAttribute("userLastName", m.getLastName());
                    }
                    if (!model.containsAttribute("userEmail") && m.getEmail() != null) {
                        model.addAttribute("userEmail", m.getEmail());
                    }
                    if (!model.containsAttribute("userFullName") && fullName != null) {
                        model.addAttribute("userFullName", fullName);
                    }
                    if (!model.containsAttribute("userInitials") && nameInitials != null) {
                        model.addAttribute("userInitials", nameInitials);
                    }
                    // Prefer display name from member if not set
                    if (displayName == null) displayName = fullName;
                }
            } catch (Exception ignored) {
                // Silently ignore issues fetching member; fall back to username
            }
        }

        String initials = computeInitials(displayName != null ? displayName : username);

        if (!model.containsAttribute("userFullName") && displayName != null) {
            model.addAttribute("userFullName", displayName);
        }
        if (!model.containsAttribute("username") && username != null) {
            model.addAttribute("username", username);
        }
        if (!model.containsAttribute("userInitials") && initials != null) {
            model.addAttribute("userInitials", initials);
        }
        // userEmail optional; templates already provide a default fallback
    }

    private String computeInitials(String name) {
        if (name == null || name.isBlank()) return null;
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(1, parts[0].length())).toUpperCase();
        }
        char first = parts[0].isEmpty() ? '?' : parts[0].charAt(0);
        char last = parts[parts.length - 1].isEmpty() ? '?' : parts[parts.length - 1].charAt(0);
        return ("" + Character.toUpperCase(first) + Character.toUpperCase(last));
    }

    private String getFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return null;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }

    private String computeInitialsFromName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return null;
        if (firstName == null) return lastName.substring(0, 1).toUpperCase();
        if (lastName == null) return firstName.substring(0, 1).toUpperCase();
        return (firstName.substring(0, 1) + lastName.substring(0, 1)).toUpperCase();
    }

    private String resolveRoleKey(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return "nav.member";
        }
        boolean admin = false;
        boolean teacher = false;
        boolean student = false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String r = a != null ? a.getAuthority() : null;
            if ("ROLE_ADMIN".equals(r)) admin = true;
            else if ("ROLE_TEACHER".equals(r)) teacher = true;
            else if ("ROLE_STUDENT".equals(r)) student = true;
        }
        if (admin) return "nav.role.admin";
        if (teacher) return "nav.role.teacher";
        if (student) return "nav.role.student";
        return "nav.member";
    }
}
