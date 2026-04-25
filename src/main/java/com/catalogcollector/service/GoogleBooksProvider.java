package com.catalogcollector.service;

import com.catalogcollector.config.ProviderProperties;
import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Order(2)
public class GoogleBooksProvider implements CatalogLookupProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksProvider.class);
    private static final Set<MediaType> SUPPORTED = Set.of(
            MediaType.MANGA, MediaType.LIGHT_NOVEL, MediaType.ART_BOOK, MediaType.MAGAZINE);

    private final String baseUrl;
    private final String apiKey;
    private final RestClient restClient;

    public GoogleBooksProvider(ProviderProperties props, RestClient restClient) {
        this.baseUrl = props.googleBooks().baseUrl();
        this.apiKey = props.googleBooks().apiKey();
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogLookupResult> lookupByBarcode(String barcode, String barcodeType) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Google Books API key not configured, skipping lookup");
            return Optional.empty();
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/books/v1/volumes?q=isbn:{isbn}&key={key}", barcode, apiKey)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Optional.empty();
            }

            Number totalItems = (Number) response.get("totalItems");
            if (totalItems == null || totalItems.intValue() == 0) {
                return Optional.empty();
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null || items.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> volumeInfo = (Map<String, Object>) items.getFirst().get("volumeInfo");
            if (volumeInfo == null) {
                return Optional.empty();
            }

            String title = (String) volumeInfo.get("title");
            String publisher = (String) volumeInfo.get("publisher");
            Integer pageCount = volumeInfo.containsKey("pageCount")
                    ? ((Number) volumeInfo.get("pageCount")).intValue() : null;
            String coverUrl = extractThumbnail(volumeInfo);
            String language = (String) volumeInfo.get("language");
            String publishedDate = (String) volumeInfo.get("publishedDate");

            return Optional.of(new CatalogLookupResult(
                    title, publisher, pageCount, coverUrl, null, language, publishedDate, null));
        } catch (Exception e) {
            log.warn("Google Books lookup failed for barcode {}: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "GoogleBooks";
    }

    @Override
    public Set<MediaType> getSupportedMediaTypes() {
        return SUPPORTED;
    }

    @SuppressWarnings("unchecked")
    private String extractThumbnail(Map<String, Object> volumeInfo) {
        Map<String, String> imageLinks = (Map<String, String>) volumeInfo.get("imageLinks");
        if (imageLinks != null) {
            return imageLinks.get("thumbnail");
        }
        return null;
    }
}
