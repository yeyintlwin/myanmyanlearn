package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    // Find OTP verification by user ID
    List<OtpVerification> findByUserId(String userId);

    // Find OTP verification by email
    List<OtpVerification> findByEmail(String email);

    // Find OTP verification by user ID and email
    List<OtpVerification> findByUserIdAndEmail(String userId, String email);

    // Find valid (non-expired) OTP verification by user ID and email
    @Query("SELECT o FROM OtpVerification o WHERE o.userId = :userId AND o.email = :email AND o.expiresAt > :now AND o.verified = false")
    Optional<OtpVerification> findValidOtpByUserIdAndEmail(@Param("userId") String userId, @Param("email") String email,
            @Param("now") LocalDateTime now);

    // Find valid OTP verification by email
    @Query("SELECT o FROM OtpVerification o WHERE o.email = :email AND o.expiresAt > :now AND o.verified = false")
    Optional<OtpVerification> findValidOtpByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    // Find OTP verification by OTP code and email
    Optional<OtpVerification> findByOtpCodeAndEmail(String otpCode, String email);

    // Find valid OTP verification by OTP code and email
    @Query("SELECT o FROM OtpVerification o WHERE o.otpCode = :otpCode AND o.email = :email AND o.expiresAt > :now AND o.verified = false")
    Optional<OtpVerification> findValidOtpByCodeAndEmail(@Param("otpCode") String otpCode, @Param("email") String email,
            @Param("now") LocalDateTime now);

    // Find expired OTP verifications
    @Query("SELECT o FROM OtpVerification o WHERE o.expiresAt < :now")
    List<OtpVerification> findExpiredOtps(@Param("now") LocalDateTime now);

    // Find verified OTP verifications
    List<OtpVerification> findByVerifiedTrue();

    // Find unverified OTP verifications
    List<OtpVerification> findByVerifiedFalse();

    // Count OTP attempts for a user
    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.userId = :userId AND o.createdAt >= :since")
    long countAttemptsByUserIdSince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    // Count OTP attempts for an email
    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.email = :email AND o.createdAt >= :since")
    long countAttemptsByEmailSince(@Param("email") String email, @Param("since") LocalDateTime since);

    // Delete expired OTP verifications
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);

    // Delete OTP verifications for a user
    void deleteByUserId(String userId);

    // Delete OTP verifications for an email
    void deleteByEmail(String email);
}
