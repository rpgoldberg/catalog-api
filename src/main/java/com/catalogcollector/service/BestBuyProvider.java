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
public class BestBuyProvider implements CatalogLookupProvider {

    private static final Logger log = LoggerFactory.getLogger(BestBuyProvider.class);
    private static final Set<MediaType> SUPPORTED = Set.of(
            MediaType.BLU_RAY, MediaType.DVD, MediaType.VHS,
            MediaType.LASERDISC, MediaType.GAME, MediaType.OTHER);

    private final String baseUrl;
    private final String apiKey;
    private final RestClient restClient;

    public BestBuyProvider(ProviderProperties props, RestClient restClient) {
        this.baseUrl = props.bestBuy().baseUrl();
        this.apiKey = props.bestBuy().apiKey();
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogLookupResult> lookupByBarcode(String barcode, String barcodeType) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Best Buy API key not configured, skipping lookup");
            return Optional.empty();
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/v1/products(upc={upc})?apiKey={key}&format=json&show=name,upc,image,largeImage,manufacturer,salePrice,releaseDate,longDescription,categoryPath",
                            barcode, apiKey)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> products = (List<Map<String, Object>>) response.get("products");
            if (products == null || products.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> product = products.getFirst();
            String title = (String) product.get("name");
            String publisher = (String) product.get("manufacturer");
            String coverImageUrl = (String) product.get("largeImage");
            if (coverImageUrl == null) {
                coverImageUrl = (String) product.get("image");
            }
            String releaseDate = (String) product.get("releaseDate");

            String description = (String) product.get("longDescription");
            String category = extractLastCategory(product);

            Map<String, Object> metadata = null;
            if (description != null || category != null) {
                metadata = new HashMap<>();
                if (description != null) {
                    metadata.put("description", description);
                }
                if (category != null) {
                    metadata.put("category", category);
                }
            }

            return Optional.of(new CatalogLookupResult(
                    title, publisher, null, coverImageUrl, null, null, releaseDate, metadata));
        } catch (Exception e) {
            log.warn("Best Buy lookup failed for barcode {}: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "BestBuy";
    }

    @Override
    public Set<MediaType> getSupportedMediaTypes() {
        return SUPPORTED;
    }

    @SuppressWarnings("unchecked")
    private String extractLastCategory(Map<String, Object> product) {
        List<Map<String, Object>> categoryPath = (List<Map<String, Object>>) product.get("categoryPath");
        if (categoryPath != null && !categoryPath.isEmpty()) {
            return (String) categoryPath.getLast().get("name");
        }
        return null;
    }
}
