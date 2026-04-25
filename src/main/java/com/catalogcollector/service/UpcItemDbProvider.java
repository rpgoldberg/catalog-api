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
@Order(5)
public class UpcItemDbProvider implements CatalogLookupProvider {

    private static final Logger log = LoggerFactory.getLogger(UpcItemDbProvider.class);
    private static final Set<MediaType> SUPPORTED = Set.of(
            MediaType.BLU_RAY, MediaType.DVD, MediaType.VHS,
            MediaType.LASERDISC, MediaType.GAME, MediaType.OTHER);

    private final String baseUrl;
    private final RestClient restClient;

    public UpcItemDbProvider(ProviderProperties props, RestClient restClient) {
        this.baseUrl = props.upcItemDb().baseUrl();
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogLookupResult> lookupByBarcode(String barcode, String barcodeType) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/prod/trial/lookup?upc={upc}", barcode)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null || items.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> item = items.getFirst();
            String title = (String) item.get("title");
            String brand = (String) item.get("brand");
            String coverUrl = extractFirstImage(item);
            String description = (String) item.get("description");

            Map<String, Object> metadata = null;
            if (description != null && !description.isBlank()) {
                metadata = new HashMap<>();
                metadata.put("description", description);
            }

            return Optional.of(new CatalogLookupResult(
                    title, brand, null, coverUrl, null, null, null, metadata));
        } catch (Exception e) {
            log.warn("UPCitemdb lookup failed for barcode {}: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "UPCitemdb";
    }

    @Override
    public Set<MediaType> getSupportedMediaTypes() {
        return SUPPORTED;
    }

    @SuppressWarnings("unchecked")
    private String extractFirstImage(Map<String, Object> item) {
        List<String> images = (List<String>) item.get("images");
        if (images != null && !images.isEmpty()) {
            return images.getFirst();
        }
        return null;
    }
}
