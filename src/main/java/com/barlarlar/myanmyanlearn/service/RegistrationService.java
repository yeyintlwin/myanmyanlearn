package com.barlarlar.myanmyanlearn.service;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.entity.Role;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class RegistrationService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpService otpService;

    /**
     * Register a new user with email verification
     */
    public Member registerUser(String userId, String password, String firstName, String lastName, String email) {
        // Check if user already exists
        if (memberRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException(
                    "Username '" + userId + "' is already taken. Please choose a different username.");
        }

        // Check if email already exists
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email '" + email + "' is already registered. Please use a different email or try logging in.");
        }

        // Create new member
        Member member = new Member();
        member.setUserId(userId);
        member.setPassword(passwordEncoder.encode(password));
        member.setActive(true);
        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setEmail(email);
        member.setEmailVerified(false);

        // Save member
        member = memberRepository.save(member);

        // Assign default role (ROLE_EMPLOYEE)
        Role role = new Role(userId, "ROLE_EMPLOYEE");
        roleRepository.save(role);

        // Generate and send OTP
        String otpCode = otpService.generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10); // 10 minutes expiry

        member.setOtpCode(otpCode);
        member.setOtpExpiresAt(expiresAt);
        memberRepository.save(member);

        // Send OTP email
        emailService.sendOtpEmail(email, otpCode);

        return member;
    }

    /**
     * Verify email with OTP
     */
    public boolean verifyEmail(String email, String otpCode) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Check if OTP is valid and not expired
        if (member.getOtpCode() == null ||
                !member.getOtpCode().equals(otpCode) ||
                member.getOtpExpiresAt() == null ||
                LocalDateTime.now().isAfter(member.getOtpExpiresAt())) {
            return false;
        }

        // Mark email as verified and clear OTP
        member.setEmailVerified(true);
        member.setOtpCode(null);
        member.setOtpExpiresAt(null);
        memberRepository.save(member);

        return true;
    }

    /**
     * Resend OTP for email verification
     */
    public boolean resendOtp(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (member.getEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        // Generate new OTP
        String otpCode = otpService.generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        member.setOtpCode(otpCode);
        member.setOtpExpiresAt(expiresAt);
        memberRepository.save(member);

        // Send new OTP email
        emailService.sendOtpEmail(email, otpCode);

        return true;
    }

    /**
     * Check if user exists by email
     */
    public boolean userExistsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    /**
     * Check if user exists by user ID
     */
    public boolean userExistsByUserId(String userId) {
        return memberRepository.existsByUserId(userId);
    }

    /**
     * Get user by email
     */
    public Member getUserByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Get user by user ID
     */
    public Member getUserByUserId(String userId) {
        return memberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with user ID: " + userId));
    }
}
