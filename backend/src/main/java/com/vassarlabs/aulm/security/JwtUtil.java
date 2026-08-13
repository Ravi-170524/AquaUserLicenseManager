package com.vassarlabs.aulm.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtUtil(@Value("${aulm.jwt.secret}") String secret,
                    @Value("${aulm.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(sha256(secret));
        this.expirationMillis = expirationMinutes * 60_000;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String generateToken(UUID userUuid) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(userUuid.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public UUID extractUserUuid(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
