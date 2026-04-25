package com.catalogcollector.integration;

import com.catalogcollector.dto.AuthResponse;
import com.catalogcollector.dto.CatalogItemRequest;
import com.catalogcollector.dto.CatalogItemResponse;
import com.catalogcollector.dto.RegisterRequest;
import com.catalogcollector.entity.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String authToken;

    @BeforeEach
    void authenticate() {
        RegisterRequest registerReq = new RegisterRequest(
                "catalog-" + System.nanoTime() + "@example.com", "Catalog User", "password123");
        ResponseEntity<AuthResponse> authResponse =
                restTemplate.postForEntity("/auth/register", registerReq, AuthResponse.class);
        authToken = authResponse.getBody().token();
    }

    @Test
    void createItem_shouldReturn201() {
        CatalogItemRequest request = new CatalogItemRequest(
                "0123456789012", "EAN13", "Dragon Ball Z Figure",
                "Bandai", null, null, null, null, null,
                MediaType.OTHER, Map.of("series", "DBZ"), null);

        HttpHeaders headers = authHeaders();
        HttpEntity<CatalogItemRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<CatalogItemResponse> response =
                restTemplate.exchange("/catalog/items", HttpMethod.POST,
                        entity, CatalogItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().canonicalTitle()).isEqualTo("Dragon Ball Z Figure");
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getItems_shouldReturnCursorPage() {
        CatalogItemRequest request = new CatalogItemRequest(
                "list-test-" + System.nanoTime(), "UPC", "Test Item",
                null, null, null, null, null, null,
                null, null, null);

        HttpHeaders headers = authHeaders();
        restTemplate.exchange("/catalog/items", HttpMethod.POST,
                new HttpEntity<>(request, headers), CatalogItemResponse.class);

        ResponseEntity<Map> response =
                restTemplate.exchange("/catalog/items?size=10", HttpMethod.GET,
                        new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("content");
        assertThat(response.getBody()).containsKey("hasMore");
        assertThat(response.getBody()).containsKey("size");
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isNotEmpty();
    }

    @Test
    void getItemById_shouldReturnItem() {
        CatalogItemRequest request = new CatalogItemRequest(
                "byid-test-" + System.nanoTime(), "UPC", "ById Item",
                null, null, null, null, null, null,
                null, null, null);

        HttpHeaders headers = authHeaders();
        ResponseEntity<CatalogItemResponse> created =
                restTemplate.exchange("/catalog/items", HttpMethod.POST,
                        new HttpEntity<>(request, headers), CatalogItemResponse.class);

        ResponseEntity<CatalogItemResponse> response =
                restTemplate.exchange("/catalog/items/" + created.getBody().id(),
                        HttpMethod.GET, new HttpEntity<>(headers), CatalogItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().canonicalTitle()).isEqualTo("ById Item");
    }

    @Test
    void lookupByBarcode_shouldBePublic() {
        String barcode = "lookup-test-" + System.nanoTime();
        CatalogItemRequest request = new CatalogItemRequest(
                barcode, "UPC", "Lookup Item",
                null, null, null, null, null, null,
                null, null, null);

        HttpHeaders headers = authHeaders();
        restTemplate.exchange("/catalog/items", HttpMethod.POST,
                new HttpEntity<>(request, headers), CatalogItemResponse.class);

        // No auth header — public endpoint
        ResponseEntity<CatalogItemResponse> response =
                restTemplate.getForEntity("/catalog/lookup/" + barcode,
                        CatalogItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().barcode()).isEqualTo(barcode);
    }

    @Test
    void lookupByBarcode_shouldReturn404ForUnknown() {
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response =
                restTemplate.getForEntity("/catalog/lookup/unknown-barcode", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createItem_shouldPersistCanonicalFields() {
        CatalogItemRequest request = new CatalogItemRequest(
                "9781234567890", "ISBN", "Manga Title", "Publisher Co",
                200, "https://img.example.com/cover.jpg", "1st",
                "ja", "2025-01-15",
                MediaType.MANGA, Map.of("volumes", 5), null);

        HttpHeaders headers = authHeaders();
        ResponseEntity<CatalogItemResponse> response =
                restTemplate.exchange("/catalog/items", HttpMethod.POST,
                        new HttpEntity<>(request, headers), CatalogItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CatalogItemResponse body = response.getBody();
        assertThat(body.canonicalTitle()).isEqualTo("Manga Title");
        assertThat(body.canonicalPublisher()).isEqualTo("Publisher Co");
        assertThat(body.canonicalPageCount()).isEqualTo(200);
        assertThat(body.mediaType()).isEqualTo(MediaType.MANGA);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        return headers;
    }
}
