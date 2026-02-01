package com.barlarlar.myanmyanlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
public class LoginAttemptEntity {

    @Id
    @Column(name = "username", length = 255, nullable = false)
    private String username;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "last_attempt")
    private LocalDateTime lastAttempt;

    @Column(name = "success", nullable = false)
    private Boolean success = false;

    @PrePersist
    public void prePersist() {
        if (attemptCount == null) {
            attemptCount = 0;
        }
        if (success == null) {
            success = false;
        }
        if (lastAttempt == null) {
            lastAttempt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (attemptCount == null) {
            attemptCount = 0;
        }
        if (success == null) {
            success = false;
        }
        if (lastAttempt == null) {
            lastAttempt = LocalDateTime.now();
        }
    }
}
