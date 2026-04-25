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
@Order(3)
public class AniListEnricher implements SecondaryEnricher {

    private static final Logger log = LoggerFactory.getLogger(AniListEnricher.class);
    private static final Set<MediaType> SUPPORTED = Set.of(MediaType.MANGA, MediaType.LIGHT_NOVEL);

    private final String baseUrl;
    private final RestClient restClient;

    public AniListEnricher(ProviderProperties props, RestClient restClient) {
        this.baseUrl = props.aniList().baseUrl();
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogLookupResult> enrichByTitle(String title, MediaType mediaType) {
        try {
            String query = """
                    query ($search: String, $type: MediaType) {
                      Media(search: $search, type: $type) {
                        title { english romaji }
                        coverImage { large }
                        description
                        genres
                        averageScore
                        volumes
                        chapters
                        startDate { year month day }
                      }
                    }
                    """;
            Map<String, Object> variables = Map.of("search", title, "type", "MANGA");
            Map<String, Object> body = Map.of("query", query, "variables", variables);

            Map<String, Object> response = restClient.post()
                    .uri(baseUrl)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Optional.empty();
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                return Optional.empty();
            }

            Map<String, Object> media = (Map<String, Object>) data.get("Media");
            if (media == null) {
                return Optional.empty();
            }

            Map<String, String> titleMap = (Map<String, String>) media.get("title");
            String resolvedTitle = titleMap.get("english") != null
                    ? titleMap.get("english") : titleMap.get("romaji");

            Map<String, String> coverImage = (Map<String, String>) media.get("coverImage");
            String coverImageUrl = coverImage != null ? coverImage.get("large") : null;

            String description = (String) media.get("description");
            List<String> genres = (List<String>) media.get("genres");
            Number averageScore = (Number) media.get("averageScore");
            Number volumes = (Number) media.get("volumes");
            Number chapters = (Number) media.get("chapters");

            Map<String, Object> metadata = new HashMap<>();
            if (description != null) {
                metadata.put("synopsis", description.replaceAll("<[^>]*>", "").trim());
            }
            if (genres != null) {
                metadata.put("genres", genres);
            }
            if (averageScore != null) {
                metadata.put("rating", averageScore);
            }
            if (volumes != null) {
                metadata.put("volumes", volumes);
            }
            if (chapters != null) {
                metadata.put("chapters", chapters);
            }

            String releaseDate = null;
            Map<String, Object> startDate = (Map<String, Object>) media.get("startDate");
            if (startDate != null) {
                Number year = (Number) startDate.get("year");
                if (year != null) {
                    Number month = (Number) startDate.get("month");
                    Number day = (Number) startDate.get("day");
                    releaseDate = String.format("%04d-%02d-%02d",
                            year.intValue(),
                            month != null ? month.intValue() : 1,
                            day != null ? day.intValue() : 1);
                }
            }

            return Optional.of(new CatalogLookupResult(
                    resolvedTitle, null, null, coverImageUrl, null, null, releaseDate,
                    metadata.isEmpty() ? null : metadata));
        } catch (Exception e) {
            log.warn("AniList lookup failed for title {}: {}", title, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "AniList";
    }

    @Override
    public Set<MediaType> getSupportedMediaTypes() {
        return SUPPORTED;
    }
}
