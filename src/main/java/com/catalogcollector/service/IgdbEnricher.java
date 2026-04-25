package com.catalogcollector.service;

import com.catalogcollector.config.ProviderProperties;
import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Order(2)
public class IgdbEnricher implements SecondaryEnricher {

    private static final Logger log = LoggerFactory.getLogger(IgdbEnricher.class);
    private static final Set<MediaType> SUPPORTED = Set.of(MediaType.GAME);

    private final String baseUrl;
    private final String clientId;
    private final RestClient restClient;
    private final OAuthTokenManager tokenManager;

    @Autowired
    public IgdbEnricher(ProviderProperties props, RestClient restClient) {
        this.baseUrl = props.igdb().baseUrl();
        this.clientId = props.igdb().clientId();
        this.restClient = restClient;
        this.tokenManager = new OAuthTokenManager(
                props.igdb().twitchTokenUrl(),
                props.igdb().clientId(),
                props.igdb().clientSecret(),
                restClient,
                false);
    }

    IgdbEnricher(String baseUrl, String clientId, RestClient restClient, OAuthTokenManager tokenManager) {
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.restClient = restClient;
        this.tokenManager = tokenManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogLookupResult> enrichByTitle(String title, MediaType mediaType) {
        if (!tokenManager.hasCredentials()) {
            return Optional.empty();
        }

        String token = tokenManager.getAccessToken();
        if (token == null) {
            return Optional.empty();
        }

        try {
            String escapedTitle = title.replace("\"", "\\\"");
            String body = "search \"" + escapedTitle + "\"; fields name,summary,cover.image_id,first_release_date; limit 1;";

            List<Map<String, Object>> response = restClient.post()
                    .uri(baseUrl + "/v4/games")
                    .header("Client-ID", clientId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                    .body(body)
                    .retrieve()
                    .body(List.class);

            if (response == null || response.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> game = response.getFirst();
            String name = (String) game.get("name");

            String coverImageUrl = null;
            Map<String, Object> cover = (Map<String, Object>) game.get("cover");
            if (cover != null) {
                String imageId = (String) cover.get("image_id");
                if (imageId != null) {
                    coverImageUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/" + imageId + ".jpg";
                }
            }

            String summary = (String) game.get("summary");
            String releaseDate = null;
            Number firstReleaseDate = (Number) game.get("first_release_date");
            if (firstReleaseDate != null) {
                releaseDate = Instant.ofEpochSecond(firstReleaseDate.longValue()).toString().substring(0, 10);
            }

            Map<String, Object> metadata = null;
            if (summary != null) {
                metadata = new HashMap<>();
                metadata.put("synopsis", summary);
            }

            return Optional.of(new CatalogLookupResult(
                    name, null, null, coverImageUrl, null, null, releaseDate, metadata));
        } catch (Exception e) {
            log.warn("IGDB lookup failed for title {}: {}", title, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "IGDB";
    }

    @Override
    public Set<MediaType> getSupportedMediaTypes() {
        return SUPPORTED;
    }
}
