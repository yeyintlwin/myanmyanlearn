package com.barlarlar.myanmyanlearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "otp_verifications")
@Getter
@Setter
@NoArgsConstructor
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "otp_code", length = 6, nullable = false)
    private String otpCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "attempts")
    private Integer attempts = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Member member;

    public OtpVerification(String userId, String email, String otpCode, LocalDateTime expiresAt) {
        this.userId = userId;
        this.email = email;
        this.otpCode = otpCode;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markAsVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "OtpVerification{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", otpCode='" + otpCode + '\'' +
                ", verified=" + verified +
                ", attempts=" + attempts +
                '}';
    }
}
