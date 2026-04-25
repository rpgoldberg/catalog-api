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
@Order(1)
public class OpenLibraryProvider implements CatalogLookupProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenLibraryProvider.class);
    private static final Set<MediaType> SUPPORTED = Set.of(
            MediaType.MANGA, MediaType.LIGHT_NOVEL, MediaType.ART_BOOK, MediaType.MAGAZINE);

    private final String baseUrl;
    private final RestClient restClient;

    public OpenLibraryProvider(ProviderProperties props, RestClient restClient) {
        this.baseUrl = props.openLibrary().baseUrl();
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogLookupResult> lookupByBarcode(String barcode, String barcodeType) {
        try {
            String key = "ISBN:" + barcode;
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/api/books?bibkeys={key}&jscmd=data&format=json", key)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey(key)) {
                return Optional.empty();
            }

            Map<String, Object> bookData = (Map<String, Object>) response.get(key);
            String title = (String) bookData.get("title");
            String publisher = extractPublisher(bookData);
            Integer pageCount = bookData.containsKey("number_of_pages")
                    ? ((Number) bookData.get("number_of_pages")).intValue() : null;
            String coverUrl = "https://covers.openlibrary.org/b/isbn/" + barcode + "-M.jpg";
            String language = extractLanguage(bookData);
            String releaseDate = (String) bookData.get("publish_date");

            return Optional.of(new CatalogLookupResult(
                    title, publisher, pageCount, coverUrl, null, language, releaseDate, null));
        } catch (Exception e) {
            log.warn("OpenLibrary lookup failed for barcode {}: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "OpenLibrary";
    }

    @Override
    public Set<MediaType> getSupportedMediaTypes() {
        return SUPPORTED;
    }

    @SuppressWarnings("unchecked")
    private String extractPublisher(Map<String, Object> bookData) {
        List<Map<String, String>> publishers = (List<Map<String, String>>) bookData.get("publishers");
        if (publishers != null && !publishers.isEmpty()) {
            return publishers.getFirst().get("name");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractLanguage(Map<String, Object> bookData) {
        List<Map<String, String>> languages = (List<Map<String, String>>) bookData.get("languages");
        if (languages != null && !languages.isEmpty()) {
            String langKey = languages.getFirst().get("key");
            if (langKey != null && langKey.startsWith("/languages/")) {
                return langKey.substring("/languages/".length());
            }
        }
        return null;
    }
}
