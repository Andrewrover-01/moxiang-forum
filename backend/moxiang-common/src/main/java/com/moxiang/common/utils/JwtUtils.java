package com.moxiang.common.utils;

import com.moxiang.common.constant.AuthConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT token utility: creation, parsing and validation.
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret:moxiang-forum-secret-key-must-be-at-least-256-bits-long-for-hs256}")
    private String secret;

    @Value("${jwt.expiration:604800}")
    private long expiration; // seconds

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a JWT token for the given user.
     *
     * @param userId   numeric user ID
     * @param username login name
     * @param role     role string (e.g. "USER" or "ADMIN")
     * @return signed JWT string
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(AuthConstants.CLAIM_USER_ID, userId);
        claims.put(AuthConstants.CLAIM_ROLE, role);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parse and return all claims from a token. Throws JwtException on invalid/expired tokens.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get(AuthConstants.CLAIM_USER_ID, Long.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get(AuthConstants.CLAIM_ROLE, String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiry = parseToken(token).getExpiration();
            return expiry.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Returns remaining TTL in seconds, or 0 if already expired.
     */
    public long getRemainingExpiry(String token) {
        try {
            Date expiry = parseToken(token).getExpiration();
            long remaining = expiry.getTime() - System.currentTimeMillis();
            return Math.max(0L, remaining / 1000);
        } catch (Exception e) {
            return 0L;
        }
    }
}
