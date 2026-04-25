package com.catalogcollector.service;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class OAuthTokenManagerTest {

    @Test
    void fetchAndCacheToken(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(okJson("""
                        {"access_token": "tok-123", "expires_in": 3600}
                        """)));

        OAuthTokenManager manager = new OAuthTokenManager(
                wmRuntimeInfo.getHttpBaseUrl() + "/oauth/token",
                "my-client-id", "my-client-secret",
                RestClient.create(), false);

        String token1 = manager.getAccessToken();
        String token2 = manager.getAccessToken();

        assertEquals("tok-123", token1);
        assertEquals("tok-123", token2);

        verify(1, postRequestedFor(urlPathEqualTo("/oauth/token")));
    }

    @Test
    void refreshExpiredToken(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(okJson("""
                        {"access_token": "new-token", "expires_in": 3600}
                        """)));

        OAuthTokenManager manager = new OAuthTokenManager(
                wmRuntimeInfo.getHttpBaseUrl() + "/oauth/token",
                "my-client-id", "my-client-secret",
                RestClient.create(), false);

        Field cachedTokenField = OAuthTokenManager.class.getDeclaredField("cachedToken");
        cachedTokenField.setAccessible(true);
        cachedTokenField.set(manager, "old");

        Field tokenExpiryField = OAuthTokenManager.class.getDeclaredField("tokenExpiry");
        tokenExpiryField.setAccessible(true);
        tokenExpiryField.set(manager, Instant.now().minusSeconds(600));

        String token = manager.getAccessToken();

        assertEquals("new-token", token);
    }

    @Test
    void failureReturnsNull(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(serverError()));

        OAuthTokenManager manager = new OAuthTokenManager(
                wmRuntimeInfo.getHttpBaseUrl() + "/oauth/token",
                "my-client-id", "my-client-secret",
                RestClient.create(), false);

        String token = manager.getAccessToken();

        assertNull(token);
    }

    @Test
    void hasCredentialsTrue() {
        OAuthTokenManager manager = new OAuthTokenManager(
                "http://unused/oauth/token",
                "my-client-id", "my-client-secret",
                RestClient.create(), false);

        assertTrue(manager.hasCredentials());
    }

    @Test
    void hasCredentialsFalse() {
        OAuthTokenManager manager = new OAuthTokenManager(
                "http://unused/oauth/token",
                "", "my-client-secret",
                RestClient.create(), false);

        assertFalse(manager.hasCredentials());
    }
}
