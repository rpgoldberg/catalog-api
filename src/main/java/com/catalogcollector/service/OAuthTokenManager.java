package com.catalogcollector.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

public class OAuthTokenManager {

    private static final Logger log = LoggerFactory.getLogger(OAuthTokenManager.class);
    private static final long REFRESH_BUFFER_SECONDS = 300; // 5 minutes before expiry

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;
    private final boolean useBasicAuth;

    private String cachedToken;
    private Instant tokenExpiry;

    public OAuthTokenManager(String tokenUrl, String clientId, String clientSecret,
                             RestClient restClient, boolean useBasicAuth) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = restClient;
        this.useBasicAuth = useBasicAuth;
    }

    public boolean hasCredentials() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    @SuppressWarnings("unchecked")
    public synchronized String getAccessToken() {
        if (cachedToken != null && tokenExpiry != null
                && Instant.now().isBefore(tokenExpiry.minusSeconds(REFRESH_BUFFER_SECONDS))) {
            return cachedToken;
        }

        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body;
            if (useBasicAuth) {
                String credentials = Base64.getEncoder()
                        .encodeToString((clientId + ":" + clientSecret).getBytes());
                request = request.header("Authorization", "Basic " + credentials);
                body = "grant_type=client_credentials&scope=https://api.ebay.com/oauth/api_scope";
            } else {
                body = "client_id=" + clientId
                        + "&client_secret=" + clientSecret
                        + "&grant_type=client_credentials";
            }

            Map<String, Object> response = request
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                log.warn("OAuth token response was null from {}", tokenUrl);
                cachedToken = null;
                return null;
            }

            cachedToken = (String) response.get("access_token");
            Number expiresIn = (Number) response.get("expires_in");
            if (expiresIn != null) {
                tokenExpiry = Instant.now().plusSeconds(expiresIn.longValue());
            } else {
                tokenExpiry = Instant.now().plusSeconds(3600);
            }

            return cachedToken;
        } catch (Exception e) {
            log.warn("Failed to obtain OAuth token from {}: {}", tokenUrl, e.getMessage());
            cachedToken = null;
            tokenExpiry = null;
            return null;
        }
    }
}
