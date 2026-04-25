package com.catalogcollector.service;

import com.catalogcollector.config.ProviderProperties;
import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Order(1)
public class TmdbEnricher implements SecondaryEnricher {

    private static final Logger log = LoggerFactory.getLogger(TmdbEnricher.class);
    private static final Set<MediaType> SUPPORTED = Set.of(
            MediaType.BLU_RAY, MediaType.DVD, MediaType.VHS, MediaType.LASERDISC);

    private final String baseUrl;
    private final String apiKey;
    private final RestClient restClient;

    public TmdbEnricher(ProviderProperties props, RestClient restClient) {
        this.baseUrl = props.tmdb().baseUrl();
        this.apiKey = props.tmdb().apiKey();
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogLookupResult> enrichByTitle(String title, MediaType mediaType) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("TMDB API key not configured, skipping lookup");
            return Optional.empty();
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/3/search/movie?query={title}", title)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> firstResult = results.getFirst();
            String movieTitle = (String) firstResult.get("title");
            String posterPath = (String) firstResult.get("poster_path");
            String coverImageUrl = posterPath != null
                    ? "https://image.tmdb.org/t/p/w500" + posterPath : null;
            String releaseDate = (String) firstResult.get("release_date");
            String overview = (String) firstResult.get("overview");
            Number voteAverage = (Number) firstResult.get("vote_average");

            Map<String, Object> metadata = null;
            if (overview != null || voteAverage != null) {
                metadata = new HashMap<>();
                if (overview != null) {
                    metadata.put("synopsis", overview);
                }
                if (voteAverage != null) {
                    metadata.put("rating", voteAverage);
                }
            }

            return Optional.of(new CatalogLookupResult(
                    movieTitle, null, null, coverImageUrl, null, null, releaseDate, metadata));
        } catch (Exception e) {
            log.warn("TMDB lookup failed for title {}: {}", title, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "TMDB";
    }

    @Override
    public Set<MediaType> getSupportedMediaTypes() {
        return SUPPORTED;
    }
}
