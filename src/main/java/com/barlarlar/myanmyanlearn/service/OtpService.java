package com.barlarlar.myanmyanlearn.service;

import com.barlarlar.myanmyanlearn.entity.OtpVerification;
import com.barlarlar.myanmyanlearn.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpVerificationRepository;

    private static final SecureRandom random = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 3;

    /**
     * Generate a 6-digit OTP
     */
    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

    /**
     * Create and save OTP verification record
     */
    public OtpVerification createOtpVerification(String userId, String email) {
        String otpCode = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        OtpVerification otpVerification = new OtpVerification(userId, email, otpCode, expiresAt);
        return otpVerificationRepository.save(otpVerification);
    }

    /**
     * Verify OTP code
     */
    public boolean verifyOtp(String email, String otpCode) {
        Optional<OtpVerification> otpOpt = otpVerificationRepository.findValidOtpByCodeAndEmail(
                otpCode, email, LocalDateTime.now());

        if (otpOpt.isEmpty()) {
            return false;
        }

        OtpVerification otpVerification = otpOpt.get();

        // Check if OTP is expired
        if (otpVerification.isExpired()) {
            return false;
        }

        // Check attempt limit
        if (otpVerification.getAttempts() >= MAX_ATTEMPTS) {
            return false;
        }

        // Mark as verified
        otpVerification.markAsVerified();
        otpVerificationRepository.save(otpVerification);

        return true;
    }

    /**
     * Verify OTP with attempt tracking
     */
    public boolean verifyOtpWithAttempts(String email, String otpCode) {
        Optional<OtpVerification> otpOpt = otpVerificationRepository.findValidOtpByEmail(
                email, LocalDateTime.now());

        if (otpOpt.isEmpty()) {
            return false;
        }

        OtpVerification otpVerification = otpOpt.get();

        // Check if OTP is expired
        if (otpVerification.isExpired()) {
            return false;
        }

        // Check attempt limit
        if (otpVerification.getAttempts() >= MAX_ATTEMPTS) {
            return false;
        }

        // Increment attempts
        otpVerification.incrementAttempts();

        // Check if OTP code matches
        if (otpVerification.getOtpCode().equals(otpCode)) {
            otpVerification.markAsVerified();
            otpVerificationRepository.save(otpVerification);
            return true;
        } else {
            otpVerificationRepository.save(otpVerification);
            return false;
        }
    }

    /**
     * Get valid OTP for email
     */
    public Optional<OtpVerification> getValidOtp(String email) {
        return otpVerificationRepository.findValidOtpByEmail(email, LocalDateTime.now());
    }

    /**
     * Get OTP verification by ID
     */
    public Optional<OtpVerification> getOtpVerification(Long id) {
        return otpVerificationRepository.findById(Objects.requireNonNull(id));
    }

    /**
     * Get all OTP verifications for a user
     */
    public List<OtpVerification> getOtpVerificationsByUserId(String userId) {
        return otpVerificationRepository.findByUserId(userId);
    }

    /**
     * Get all OTP verifications for an email
     */
    public List<OtpVerification> getOtpVerificationsByEmail(String email) {
        return otpVerificationRepository.findByEmail(email);
    }

    /**
     * Check if OTP exists and is valid
     */
    public boolean isOtpValid(String email, String otpCode) {
        Optional<OtpVerification> otpOpt = otpVerificationRepository.findValidOtpByCodeAndEmail(
                otpCode, email, LocalDateTime.now());

        if (otpOpt.isEmpty()) {
            return false;
        }

        OtpVerification otpVerification = otpOpt.get();
        return !otpVerification.isExpired() &&
                otpVerification.getAttempts() < MAX_ATTEMPTS &&
                otpVerification.getOtpCode().equals(otpCode);
    }

    /**
     * Clean up expired OTPs
     */
    public void cleanupExpiredOtps() {
        otpVerificationRepository.deleteExpiredOtps(LocalDateTime.now());
    }

    /**
     * Get attempt count for user
     */
    public long getAttemptCount(String userId, LocalDateTime since) {
        return otpVerificationRepository.countAttemptsByUserIdSince(userId, since);
    }

    /**
     * Get attempt count for email
     */
    public long getAttemptCountByEmail(String email, LocalDateTime since) {
        return otpVerificationRepository.countAttemptsByEmailSince(email, since);
    }

    /**
     * Delete OTP verifications for user
     */
    public void deleteOtpsForUser(String userId) {
        otpVerificationRepository.deleteByUserId(userId);
    }

    /**
     * Delete OTP verifications for email
     */
    public void deleteOtpsForEmail(String email) {
        otpVerificationRepository.deleteByEmail(email);
    }
}
