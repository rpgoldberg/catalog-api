package com.catalogcollector.service;

import com.catalogcollector.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha!!", 86400000L);
        jwtService = new JwtService(props);
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "test@example.com");

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    void extractUserId_shouldReturnOriginalUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "test@example.com");

        UUID extracted = jwtService.extractUserId(token);

        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void extractEmail_shouldReturnOriginalEmail() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = jwtService.generateToken(userId, email);

        String extracted = jwtService.extractEmail(token);

        assertThat(extracted).isEqualTo(email);
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "test@example.com");

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        JwtProperties expiredProps = new JwtProperties(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha!!", -1000L);
        JwtService expiredService = new JwtService(expiredProps);

        String token = expiredService.generateToken(UUID.randomUUID(), "test@example.com");

        assertThat(expiredService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForGarbageToken() {
        assertThat(jwtService.isTokenValid("not.a.valid.token")).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForTokenWithWrongKey() {
        JwtProperties otherProps = new JwtProperties(
                "different-secret-key-that-is-at-least-256-bits-long-for-hmac!!", 86400000L);
        JwtService otherService = new JwtService(otherProps);

        String token = otherService.generateToken(UUID.randomUUID(), "test@example.com");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
