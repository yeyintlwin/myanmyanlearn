package com.barlarlar.myanmyanlearn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.barlarlar.myanmyanlearn.entity.LoginAttemptEntity;
import com.barlarlar.myanmyanlearn.repository.LoginAttemptRepository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;

    // In-memory cache for login attempts (for rate limiting)
    private final ConcurrentHashMap<String, AtomicInteger> attemptCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastAttemptTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lockoutTimes = new ConcurrentHashMap<>();

    // Configuration constants
    private static final int MAX_ATTEMPTS_PER_IP = 5; // Max attempts per IP in time window
    private static final int TIME_WINDOW_MINUTES = 15; // Time window for rate limiting
    private static final int LOCKOUT_DURATION_MINUTES = 30; // Account lockout duration
    private static final int MAX_ATTEMPTS_BEFORE_LOCKOUT = 5; // Failed attempts before lockout

    /**
     * Check if IP is rate limited
     */
    public boolean isIpRateLimited(String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(TIME_WINDOW_MINUTES);

        // Clean old entries
        attemptCounts.entrySet()
                .removeIf(entry -> lastAttemptTimes.getOrDefault(entry.getKey(), now).isBefore(windowStart));

        AtomicInteger attempts = attemptCounts.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
        LocalDateTime lastAttempt = lastAttemptTimes.getOrDefault(ipAddress, now);

        // Reset counter if outside time window
        if (lastAttempt.isBefore(windowStart)) {
            attempts.set(0);
        }

        return attempts.get() >= MAX_ATTEMPTS_PER_IP;
    }

    /**
     * Check if user account is locked
     */
    public boolean isUserLocked(String username) {
        LocalDateTime lockoutTime = lockoutTimes.get(username);
        if (lockoutTime != null) {
            if (LocalDateTime.now().isBefore(lockoutTime.plusMinutes(LOCKOUT_DURATION_MINUTES))) {
                return true; // Still locked
            } else {
                lockoutTimes.remove(username); // Lockout expired
            }
        }
        return false;
    }

    /**
     * Record a failed login attempt
     */
    public void recordFailedAttempt(String ipAddress, String username) {
        LocalDateTime now = LocalDateTime.now();

        // Record IP attempt
        AtomicInteger ipAttempts = attemptCounts.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
        ipAttempts.incrementAndGet();
        lastAttemptTimes.put(ipAddress, now);

        // Record user attempt in database
        recordUserFailedAttempt(username);

        // Check if user should be locked
        int userAttempts = getUserFailedAttempts(username);
        if (userAttempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            lockoutTimes.put(username, now);
            log.warn("User {} has been locked due to too many failed attempts", username);
        }
    }

    /**
     * Record a successful login attempt
     */
    public void recordSuccessfulAttempt(String ipAddress, String username) {
        // Clear IP attempts
        attemptCounts.remove(ipAddress);
        lastAttemptTimes.remove(ipAddress);

        // Clear user lockout
        lockoutTimes.remove(username);

        // Reset user failed attempts in database
        resetUserFailedAttempts(username);
    }

    /**
     * Get remaining time until IP rate limit resets
     */
    public int getIpRateLimitRemainingMinutes(String ipAddress) {
        LocalDateTime lastAttempt = lastAttemptTimes.get(ipAddress);
        if (lastAttempt == null)
            return 0;

        LocalDateTime resetTime = lastAttempt.plusMinutes(TIME_WINDOW_MINUTES);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(resetTime))
            return 0;

        return (int) java.time.Duration.between(now, resetTime).toMinutes();
    }

    /**
     * Get remaining time until user lockout expires
     */
    public int getUserLockoutRemainingMinutes(String username) {
        LocalDateTime lockoutTime = lockoutTimes.get(username);
        if (lockoutTime == null)
            return 0;

        LocalDateTime unlockTime = lockoutTime.plusMinutes(LOCKOUT_DURATION_MINUTES);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(unlockTime))
            return 0;

        return (int) java.time.Duration.between(now, unlockTime).toMinutes();
    }

    /**
     * Record user failed attempt in database
     */
    private void recordUserFailedAttempt(String username) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LoginAttemptEntity entity = loginAttemptRepository.findById(Objects.requireNonNull(username))
                    .orElseGet(() -> {
                        LoginAttemptEntity e = new LoginAttemptEntity();
                        e.setUsername(username);
                        e.setAttemptCount(0);
                        return e;
                    });
            int nextCount = (entity.getAttemptCount() != null ? entity.getAttemptCount() : 0) + 1;
            entity.setAttemptCount(nextCount);
            entity.setLastAttempt(now);
            entity.setSuccess(false);
            loginAttemptRepository.save(entity);
        } catch (Exception e) {
            log.error("Error recording failed attempt for user {}", username, e);
        }
    }

    /**
     * Get user failed attempts count
     */
    private int getUserFailedAttempts(String username) {
        try {
            return loginAttemptRepository.findById(Objects.requireNonNull(username))
                    .filter(a -> a.getLastAttempt() != null
                            && !a.getLastAttempt().isBefore(LocalDateTime.now().minusMinutes(TIME_WINDOW_MINUTES)))
                    .map(a -> a.getAttemptCount() != null ? a.getAttemptCount() : 0)
                    .orElse(0);
        } catch (Exception e) {
            log.error("Error getting failed attempts for user {}", username, e);
            return 0;
        }
    }

    /**
     * Reset user failed attempts
     */
    private void resetUserFailedAttempts(String username) {
        try {
            loginAttemptRepository.deleteById(Objects.requireNonNull(username));
        } catch (Exception e) {
            log.error("Error resetting failed attempts for user {}", username, e);
        }
    }

    public void deleteAttemptsForUser(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        lockoutTimes.remove(username);
        resetUserFailedAttempts(username);
    }
}
