package com.agentskills.sharing.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // Secret must be at least 256 bits (32 bytes) for HMAC-SHA256
        String secret = "test-secret-key-must-be-at-least-256-bits-long!!";
        jwtUtil = new JwtUtil(secret, 7);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String token = jwtUtil.generateToken("user-123", "testuser", "https://avatar.url/pic.png");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void shouldExtractClaimsFromToken() {
        String userId = "user-456";
        String username = "johndoe";
        String avatarUrl = "https://github.com/avatar.png";

        String token = jwtUtil.generateToken(userId, username, avatarUrl);

        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(username);
        assertThat(jwtUtil.extractAvatarUrl(token)).isEqualTo(avatarUrl);
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThat(jwtUtil.validateToken("invalid.token.here")).isFalse();
        assertThat(jwtUtil.validateToken("")).isFalse();
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        // Create a JwtUtil with 0 days expiration
        JwtUtil expiredJwtUtil = new JwtUtil(
                "test-secret-key-must-be-at-least-256-bits-long!!", 0);
        String token = expiredJwtUtil.generateToken("user-1", "user", "url");

        // Token with 0 days expiration should be expired immediately
        // (issuedAt == expiration, so it's expired)
        assertThat(expiredJwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() {
        JwtUtil otherJwtUtil = new JwtUtil(
                "different-secret-key-also-must-be-at-least-256-bits!", 7);
        String token = otherJwtUtil.generateToken("user-1", "user", "url");

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }
}
