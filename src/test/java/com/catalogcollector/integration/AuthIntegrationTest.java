package com.catalogcollector.integration;

import com.catalogcollector.dto.AuthResponse;
import com.catalogcollector.dto.LoginRequest;
import com.catalogcollector.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void register_shouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest(
                "newuser@example.com", "New User", "password123");

        ResponseEntity<AuthResponse> response =
                restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotEmpty();
        assertThat(response.getBody().email()).isEqualTo("newuser@example.com");
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "duplicate@example.com", "User One", "password123");
        restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);

        RegisterRequest duplicate = new RegisterRequest(
                "duplicate@example.com", "User Two", "password456");

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response =
                restTemplate.postForEntity("/api/auth/register", duplicate, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        RegisterRequest registerReq = new RegisterRequest(
                "logintest@example.com", "Login User", "password123");
        restTemplate.postForEntity("/api/auth/register", registerReq, AuthResponse.class);

        LoginRequest loginReq = new LoginRequest("logintest@example.com", "password123");

        ResponseEntity<AuthResponse> response =
                restTemplate.postForEntity("/api/auth/login", loginReq, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotEmpty();
    }

    @Test
    void login_shouldRejectInvalidPassword() {
        RegisterRequest registerReq = new RegisterRequest(
                "badpasstest@example.com", "User", "password123");
        restTemplate.postForEntity("/api/auth/register", registerReq, AuthResponse.class);

        LoginRequest loginReq = new LoginRequest("badpasstest@example.com", "wrongpassword");

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response =
                restTemplate.postForEntity("/api/auth/login", loginReq, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_shouldRejectInvalidEmail() {
        RegisterRequest request = new RegisterRequest(
                "not-an-email", "User", "password123");

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response =
                restTemplate.postForEntity("/api/auth/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_shouldRejectShortPassword() {
        RegisterRequest request = new RegisterRequest(
                "short@example.com", "User", "short");

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response =
                restTemplate.postForEntity("/api/auth/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
