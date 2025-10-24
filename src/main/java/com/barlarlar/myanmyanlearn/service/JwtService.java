package com.barlarlar.myanmyanlearn.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final SecretKey resetSecretKey;
    private final long resetExpiryMillis;

    public JwtService(
            @Value("${app.jwt.reset.secret}") String resetSecret,
            @Value("${app.jwt.reset.expiry-minutes}") long resetExpiryMinutes) {
        // Use raw bytes for JWT key
        this.resetSecretKey = Keys.hmacShaKeyFor(resetSecret.getBytes(StandardCharsets.UTF_8));
        this.resetExpiryMillis = resetExpiryMinutes * 60_000L;
    }

    public String generatePasswordResetToken(String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject("password-reset")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(resetExpiryMillis)))
                .addClaims(Map.of("email", email))
                .signWith(resetSecretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String parsePasswordResetEmail(String token) {
        var jwt = Jwts.parserBuilder()
                .setSigningKey(resetSecretKey)
                .build()
                .parseClaimsJws(token);
        Object email = jwt.getBody().get("email");
        return email == null ? null : email.toString();
    }
}
