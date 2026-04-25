package com.catalogcollector.integration;

import com.catalogcollector.dto.AuthResponse;
import com.catalogcollector.dto.CatalogItemRequest;
import com.catalogcollector.dto.CatalogItemResponse;
import com.catalogcollector.dto.CollectionEntryRequest;
import com.catalogcollector.dto.CollectionEntryResponse;
import com.catalogcollector.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String authToken;
    private UUID catalogItemId;

    @BeforeEach
    void setUp() {
        // Register user
        RegisterRequest registerReq = new RegisterRequest(
                "collection-" + System.nanoTime() + "@example.com",
                "Collection User", "password123");
        ResponseEntity<AuthResponse> authResponse =
                restTemplate.postForEntity("/auth/register", registerReq, AuthResponse.class);
        authToken = authResponse.getBody().token();

        // Create a catalog item
        CatalogItemRequest itemReq = new CatalogItemRequest(
                "col-barcode-" + System.nanoTime(), "UPC", "Test Collectible",
                "TestBrand", null, null, null, null, null,
                null, null, null);
        HttpHeaders headers = authHeaders();
        ResponseEntity<CatalogItemResponse> itemResponse =
                restTemplate.exchange("/catalog/items", HttpMethod.POST,
                        new HttpEntity<>(itemReq, headers), CatalogItemResponse.class);
        catalogItemId = itemResponse.getBody().id();
    }

    @Test
    void createCollectionEntry_shouldReturn201() {
        CollectionEntryRequest request = new CollectionEntryRequest(
                catalogItemId, "Mint", new BigDecimal("49.99"),
                LocalDate.of(2025, 3, 15), "Sealed in box", 1);

        HttpHeaders headers = authHeaders();
        ResponseEntity<CollectionEntryResponse> response =
                restTemplate.exchange("/collections", HttpMethod.POST,
                        new HttpEntity<>(request, headers), CollectionEntryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().condition()).isEqualTo("Mint");
        assertThat(response.getBody().catalogItemTitle()).isEqualTo("Test Collectible");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserCollection_shouldReturnCursorPage() {
        CollectionEntryRequest request = new CollectionEntryRequest(
                catalogItemId, "Good", null, null, null, 2);

        HttpHeaders headers = authHeaders();
        restTemplate.exchange("/collections", HttpMethod.POST,
                new HttpEntity<>(request, headers), CollectionEntryResponse.class);

        ResponseEntity<Map> response =
                restTemplate.exchange("/collections?size=10", HttpMethod.GET,
                        new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("content");
        assertThat(response.getBody()).containsKey("hasMore");
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isNotEmpty();
    }

    @Test
    void getCollectionEntry_shouldReturnSingleEntry() {
        CollectionEntryRequest request = new CollectionEntryRequest(
                catalogItemId, "Fair", null, null, null, 1);

        HttpHeaders headers = authHeaders();
        ResponseEntity<CollectionEntryResponse> created =
                restTemplate.exchange("/collections", HttpMethod.POST,
                        new HttpEntity<>(request, headers), CollectionEntryResponse.class);

        ResponseEntity<CollectionEntryResponse> response =
                restTemplate.exchange("/collections/" + created.getBody().id(),
                        HttpMethod.GET, new HttpEntity<>(headers), CollectionEntryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().condition()).isEqualTo("Fair");
    }

    @Test
    void updateCollectionEntry_shouldModifyEntry() {
        CollectionEntryRequest createReq = new CollectionEntryRequest(
                catalogItemId, "Good", new BigDecimal("10.00"), null, null, 1);

        HttpHeaders headers = authHeaders();
        ResponseEntity<CollectionEntryResponse> created =
                restTemplate.exchange("/collections", HttpMethod.POST,
                        new HttpEntity<>(createReq, headers), CollectionEntryResponse.class);

        CollectionEntryRequest updateReq = new CollectionEntryRequest(
                catalogItemId, "Mint", new BigDecimal("25.00"),
                LocalDate.of(2025, 6, 1), "Upgraded condition", 1);

        ResponseEntity<CollectionEntryResponse> response =
                restTemplate.exchange("/collections/" + created.getBody().id(),
                        HttpMethod.PUT, new HttpEntity<>(updateReq, headers),
                        CollectionEntryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().condition()).isEqualTo("Mint");
        assertThat(response.getBody().purchasePrice()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void deleteCollectionEntry_shouldReturn204() {
        CollectionEntryRequest request = new CollectionEntryRequest(
                catalogItemId, null, null, null, null, 1);

        HttpHeaders headers = authHeaders();
        ResponseEntity<CollectionEntryResponse> created =
                restTemplate.exchange("/collections", HttpMethod.POST,
                        new HttpEntity<>(request, headers), CollectionEntryResponse.class);

        ResponseEntity<Void> response =
                restTemplate.exchange("/collections/" + created.getBody().id(),
                        HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void collectionEndpoints_shouldRequireAuthentication() {
        // No auth header
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response =
                restTemplate.getForEntity("/collections", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getEntry_shouldReturn404ForOtherUsersEntry() {
        // Create entry as first user
        CollectionEntryRequest request = new CollectionEntryRequest(
                catalogItemId, null, null, null, null, 1);
        HttpHeaders headers = authHeaders();
        ResponseEntity<CollectionEntryResponse> created =
                restTemplate.exchange("/collections", HttpMethod.POST,
                        new HttpEntity<>(request, headers), CollectionEntryResponse.class);

        // Register second user
        RegisterRequest registerReq = new RegisterRequest(
                "other-" + System.nanoTime() + "@example.com",
                "Other User", "password123");
        ResponseEntity<AuthResponse> otherAuth =
                restTemplate.postForEntity("/auth/register", registerReq, AuthResponse.class);

        HttpHeaders otherHeaders = new HttpHeaders();
        otherHeaders.setBearerAuth(otherAuth.getBody().token());

        // Try to access first user's entry
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response =
                restTemplate.exchange("/collections/" + created.getBody().id(),
                        HttpMethod.GET, new HttpEntity<>(otherHeaders), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        return headers;
    }
}
