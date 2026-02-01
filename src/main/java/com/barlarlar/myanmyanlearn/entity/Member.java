package com.barlarlar.myanmyanlearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
public class Member {

    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "pw", length = 68, nullable = false)
    private String password;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "current_class", length = 100)
    private String currentClass;

    @Column(name = "school_year", length = 20)
    private String schoolYear;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Role> roles;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OtpVerification> otpVerifications;

    public Member(String userId, String password, Boolean active) {
        this.userId = userId;
        this.password = password;
        this.active = active;
    }

    @Override
    public String toString() {
        return "Member{" +
                "userId='" + userId + '\'' +
                ", active=" + active +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", emailVerified=" + emailVerified +
                '}';
    }
}
