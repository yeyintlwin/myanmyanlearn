package com.barlarlar.myanmyanlearn.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "password_reset_token", indexes = {
        @Index(name = "idx_password_reset_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "expire_time", nullable = false)
    private Instant expireTime;

    public PasswordResetToken(String token, String email, Instant expireTime) {
        this.token = token;
        this.email = email;
        this.expireTime = expireTime;
    }
}
