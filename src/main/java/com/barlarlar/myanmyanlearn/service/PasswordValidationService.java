package com.barlarlar.myanmyanlearn.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

@Service
public class PasswordValidationService {

    // Password strength requirements
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    private static final Pattern COMMON_PASSWORDS_PATTERN = Pattern.compile(
            "^(password|123456|123456789|qwerty|abc123|password123|admin|letmein|welcome|monkey)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Validate password strength
     */
    public PasswordValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int score = 0;

        if (password == null || password.isEmpty()) {
            errors.add("Password is required");
            return new PasswordValidationResult(false, errors, warnings, 0);
        }

        // Length validation
        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        } else {
            score += 1;
        }

        if (password.length() > MAX_LENGTH) {
            errors.add("Password must be no more than " + MAX_LENGTH + " characters long");
        }

        // Character type validation
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        } else {
            score += 1;
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        } else {
            score += 1;
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one number");
        } else {
            score += 1;
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            warnings.add("Password should contain at least one special character (!@#$%^&*()_+-=[]{}|;':\",./<>?)");
        } else {
            score += 1;
        }

        // Common password check
        if (COMMON_PASSWORDS_PATTERN.matcher(password).find()) {
            errors.add("Password is too common. Please choose a more unique password");
        }

        // Sequential characters check
        if (hasSequentialCharacters(password)) {
            warnings.add("Password contains sequential characters which may be less secure");
        }

        // Repeated characters check
        if (hasRepeatedCharacters(password)) {
            warnings.add("Password contains repeated characters which may be less secure");
        }

        // Calculate strength level
        String strength = calculateStrength(score, password.length());

        return new PasswordValidationResult(errors.isEmpty(), errors, warnings, score, strength);
    }

    /**
     * Check for sequential characters (e.g., "abc", "123")
     */
    private boolean hasSequentialCharacters(String password) {
        String lowerPassword = password.toLowerCase();
        for (int i = 0; i < lowerPassword.length() - 2; i++) {
            char c1 = lowerPassword.charAt(i);
            char c2 = lowerPassword.charAt(i + 1);
            char c3 = lowerPassword.charAt(i + 2);

            if (c2 == c1 + 1 && c3 == c2 + 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for repeated characters (e.g., "aaa")
     */
    private boolean hasRepeatedCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c = password.charAt(i);
            if (c == password.charAt(i + 1) && c == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate password strength
     */
    private String calculateStrength(int score, int length) {
        if (score < 3) {
            return "Very Weak";
        } else if (score < 4) {
            return "Weak";
        } else if (score < 5) {
            return "Medium";
        } else if (score < 6) {
            return "Strong";
        } else {
            return "Very Strong";
        }
    }

    /**
     * Password validation result class
     */
    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        private final int score;
        private final String strength;

        public PasswordValidationResult(boolean valid, List<String> errors, List<String> warnings, int score) {
            this(valid, errors, warnings, score, "Unknown");
        }

        public PasswordValidationResult(boolean valid, List<String> errors, List<String> warnings, int score,
                String strength) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
            this.score = score;
            this.strength = strength;
        }

        // Getters
        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public int getScore() {
            return score;
        }

        public String getStrength() {
            return strength;
        }
    }
}
