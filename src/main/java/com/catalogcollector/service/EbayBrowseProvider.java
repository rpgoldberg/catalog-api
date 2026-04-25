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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Order(6)
public class EbayBrowseProvider implements CatalogLookupProvider {

    private static final Logger log = LoggerFactory.getLogger(EbayBrowseProvider.class);
    private static final Set<MediaType> SUPPORTED = Set.of(MediaType.values());

    private final String baseUrl;
    private final RestClient restClient;
    private final OAuthTokenManager tokenManager;

    @Autowired
    public EbayBrowseProvider(ProviderProperties props, RestClient restClient) {
        this.baseUrl = props.ebayBrowse().baseUrl();
        this.restClient = restClient;
        this.tokenManager = new OAuthTokenManager(
                props.ebayBrowse().tokenUrl(),
                props.ebayBrowse().clientId(),
                props.ebayBrowse().clientSecret(),
                restClient,
                true);
    }

    EbayBrowseProvider(String baseUrl, RestClient restClient, OAuthTokenManager tokenManager) {
        this.baseUrl = baseUrl;
        this.restClient = restClient;
        this.tokenManager = tokenManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogLookupResult> lookupByBarcode(String barcode, String barcodeType) {
        if (!tokenManager.hasCredentials()) {
            log.debug("eBay Browse API credentials not configured, skipping lookup");
            return Optional.empty();
        }

        String token = tokenManager.getAccessToken();
        if (token == null) {
            return Optional.empty();
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/buy/browse/v1/item_summary/search?gtin={gtin}&limit=3", barcode)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> itemSummaries =
                    (List<Map<String, Object>>) response.get("itemSummaries");
            if (itemSummaries == null || itemSummaries.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> summary = itemSummaries.getFirst();
            String title = (String) summary.get("title");

            String coverImageUrl = null;
            Map<String, Object> image = (Map<String, Object>) summary.get("image");
            if (image != null) {
                coverImageUrl = (String) image.get("imageUrl");
            }

            String category = null;
            List<Map<String, Object>> categories =
                    (List<Map<String, Object>>) summary.get("categories");
            if (categories != null && !categories.isEmpty()) {
                category = (String) categories.getFirst().get("categoryName");
            }

            Map<String, Object> metadata = null;
            if (category != null) {
                metadata = new HashMap<>();
                metadata.put("category", category);
            }

            return Optional.of(new CatalogLookupResult(
                    title, null, null, coverImageUrl, null, null, null, metadata));
        } catch (Exception e) {
            log.warn("eBay Browse lookup failed for barcode {}: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "eBay";
    }

    @Override
    public Set<MediaType> getSupportedMediaTypes() {
        return SUPPORTED;
    }
}
