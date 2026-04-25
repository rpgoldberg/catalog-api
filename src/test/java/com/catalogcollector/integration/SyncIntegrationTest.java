package com.catalogcollector.integration;

import com.catalogcollector.dto.AuthResponse;
import com.catalogcollector.dto.CatalogItemRequest;
import com.catalogcollector.dto.CatalogItemResponse;
import com.catalogcollector.dto.DeltaSyncResponse;
import com.catalogcollector.dto.RegisterRequest;
import com.catalogcollector.dto.SyncMutation;
import com.catalogcollector.dto.SyncMutation.MutationType;
import com.catalogcollector.dto.SyncPushRequest;
import com.catalogcollector.dto.SyncPushResponse;
import com.catalogcollector.dto.SyncStatusResponse;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SyncIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String authToken;
    private UUID catalogItemId;

    @BeforeEach
    void setUp() {
        RegisterRequest registerReq = new RegisterRequest(
                "sync-" + System.nanoTime() + "@example.com", "Sync User", "password123");
        ResponseEntity<AuthResponse> authResponse =
                restTemplate.postForEntity("/auth/register", registerReq, AuthResponse.class);
        authToken = authResponse.getBody().token();

        // Create a catalog item for collection tests
        CatalogItemRequest itemReq = new CatalogItemRequest(
                "sync-barcode-" + System.nanoTime(), "UPC", "Sync Test Item",
                null, null, null, null, null, null,
                null, null, null);
        HttpHeaders headers = authHeaders();
        ResponseEntity<CatalogItemResponse> itemResp =
                restTemplate.exchange("/catalog/items", HttpMethod.POST,
                        new HttpEntity<>(itemReq, headers), CatalogItemResponse.class);
        catalogItemId = itemResp.getBody().id();
    }

    // --- GET /sync/delta ---

    @Test
    void getDelta_shouldReturnChangedRecords() {
        String since = "2020-01-01T00:00:00Z";
        HttpHeaders headers = authHeaders();

        ResponseEntity<DeltaSyncResponse> response =
                restTemplate.exchange("/sync/delta?since=" + since,
                        HttpMethod.GET, new HttpEntity<>(headers), DeltaSyncResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DeltaSyncResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.catalogItems()).isNotEmpty();
        assertThat(body.serverTimestamp()).isNotNull();
    }

    @Test
    void getDelta_shouldRespectLimitParameter() {
        // Create multiple items
        HttpHeaders headers = authHeaders();
        for (int i = 0; i < 3; i++) {
            CatalogItemRequest req = new CatalogItemRequest(
                    "delta-limit-" + System.nanoTime(), "UPC", "Limit Test " + i,
                    null, null, null, null, null, null,
                    null, null, null);
            restTemplate.exchange("/catalog/items", HttpMethod.POST,
                    new HttpEntity<>(req, headers), CatalogItemResponse.class);
        }

        ResponseEntity<DeltaSyncResponse> response =
                restTemplate.exchange("/sync/delta?since=2020-01-01T00:00:00Z&limit=2",
                        HttpMethod.GET, new HttpEntity<>(headers), DeltaSyncResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().catalogItems().size()).isLessThanOrEqualTo(2);
        assertThat(response.getBody().hasMore()).isTrue();
    }

    @Test
    void getDelta_shouldRequireAuthentication() {
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/sync/delta?since=2020-01-01T00:00:00Z", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- POST /sync/push ---

    @Test
    void push_shouldCreateItemFromMutation() {
        SyncMutation mutation = new SyncMutation(MutationType.CREATE_ITEM,
                UUID.randomUUID().toString(),
                Map.of("barcode", "push-isbn-" + System.nanoTime(),
                        "barcodeType", "ISBN",
                        "canonicalTitle", "Pushed Book",
                        "mediaType", "MANGA"),
                Instant.now());

        SyncPushRequest request = new SyncPushRequest(List.of(mutation));
        HttpHeaders headers = authHeaders();

        ResponseEntity<SyncPushResponse> response =
                restTemplate.exchange("/sync/push", HttpMethod.POST,
                        new HttpEntity<>(request, headers), SyncPushResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().results()).hasSize(1);
        assertThat(response.getBody().results().getFirst().status().name()).isEqualTo("CREATED");
        assertThat(response.getBody().results().getFirst().serverId()).isNotNull();
    }

    @Test
    void push_shouldAddToCollectionFromMutation() {
        SyncMutation mutation = new SyncMutation(MutationType.ADD_TO_COLLECTION,
                UUID.randomUUID().toString(),
                Map.of("catalogItemId", catalogItemId.toString(),
                        "condition", "Mint",
                        "quantity", 1),
                Instant.now());

        SyncPushRequest request = new SyncPushRequest(List.of(mutation));
        HttpHeaders headers = authHeaders();

        ResponseEntity<SyncPushResponse> response =
                restTemplate.exchange("/sync/push", HttpMethod.POST,
                        new HttpEntity<>(request, headers), SyncPushResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().results().getFirst().status().name()).isEqualTo("CREATED");
    }

    @Test
    void push_shouldBeIdempotentForDuplicateClientId() {
        String clientId = UUID.randomUUID().toString();
        SyncMutation mutation = new SyncMutation(MutationType.CREATE_ITEM, clientId,
                Map.of("barcode", "idem-" + System.nanoTime(),
                        "barcodeType", "UPC",
                        "canonicalTitle", "Idempotent Item"),
                Instant.now());

        HttpHeaders headers = authHeaders();
        SyncPushRequest request = new SyncPushRequest(List.of(mutation));

        // First push
        ResponseEntity<SyncPushResponse> first =
                restTemplate.exchange("/sync/push", HttpMethod.POST,
                        new HttpEntity<>(request, headers), SyncPushResponse.class);
        assertThat(first.getBody().results().getFirst().status().name()).isEqualTo("CREATED");

        // Second push (same clientId)
        ResponseEntity<SyncPushResponse> second =
                restTemplate.exchange("/sync/push", HttpMethod.POST,
                        new HttpEntity<>(request, headers), SyncPushResponse.class);
        assertThat(second.getBody().results().getFirst().status().name()).isEqualTo("CONFLICT");
        assertThat(second.getBody().results().getFirst().serverId())
                .isEqualTo(first.getBody().results().getFirst().serverId());
    }

    @Test
    void push_shouldHandleMultipleMutations() {
        SyncMutation create1 = new SyncMutation(MutationType.CREATE_ITEM,
                UUID.randomUUID().toString(),
                Map.of("barcode", "multi-1-" + System.nanoTime(),
                        "barcodeType", "UPC", "canonicalTitle", "Multi 1"),
                Instant.now());
        SyncMutation create2 = new SyncMutation(MutationType.CREATE_ITEM,
                UUID.randomUUID().toString(),
                Map.of("barcode", "multi-2-" + System.nanoTime(),
                        "barcodeType", "UPC", "canonicalTitle", "Multi 2"),
                Instant.now());

        SyncPushRequest request = new SyncPushRequest(List.of(create1, create2));
        HttpHeaders headers = authHeaders();

        ResponseEntity<SyncPushResponse> response =
                restTemplate.exchange("/sync/push", HttpMethod.POST,
                        new HttpEntity<>(request, headers), SyncPushResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().results()).hasSize(2);
        assertThat(response.getBody().results().get(0).status().name()).isEqualTo("CREATED");
        assertThat(response.getBody().results().get(1).status().name()).isEqualTo("CREATED");
    }

    @Test
    void push_shouldSoftDeleteCollectionEntry() {
        // First add an entry
        String addClientId = UUID.randomUUID().toString();
        SyncMutation addMutation = new SyncMutation(MutationType.ADD_TO_COLLECTION,
                addClientId,
                Map.of("catalogItemId", catalogItemId.toString(), "quantity", 1),
                Instant.now());
        HttpHeaders headers = authHeaders();
        ResponseEntity<SyncPushResponse> addResp =
                restTemplate.exchange("/sync/push", HttpMethod.POST,
                        new HttpEntity<>(new SyncPushRequest(List.of(addMutation)), headers),
                        SyncPushResponse.class);
        UUID entryId = addResp.getBody().results().getFirst().serverId();

        // Now delete it
        SyncMutation deleteMutation = new SyncMutation(MutationType.DELETE_FROM_COLLECTION,
                UUID.randomUUID().toString(),
                Map.of("entryId", entryId.toString()),
                Instant.now());
        ResponseEntity<SyncPushResponse> delResp =
                restTemplate.exchange("/sync/push", HttpMethod.POST,
                        new HttpEntity<>(new SyncPushRequest(List.of(deleteMutation)), headers),
                        SyncPushResponse.class);

        assertThat(delResp.getBody().results().getFirst().status().name()).isEqualTo("DELETED");
    }

    @Test
    void push_shouldRequireAuthentication() {
        SyncPushRequest request = new SyncPushRequest(List.of(
                new SyncMutation(MutationType.CREATE_ITEM, "x",
                        Map.of("barcode", "x", "barcodeType", "UPC"), Instant.now())));

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response =
                restTemplate.postForEntity("/sync/push", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- GET /sync/status ---

    @Test
    void getStatus_shouldReturnSyncStatus() {
        HttpHeaders headers = authHeaders();

        ResponseEntity<SyncStatusResponse> response =
                restTemplate.exchange("/sync/status", HttpMethod.GET,
                        new HttpEntity<>(headers), SyncStatusResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SyncStatusResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.serverTimestamp()).isNotNull();
        assertThat(body.catalogItemCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getStatus_shouldRequireAuthentication() {
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response =
                restTemplate.getForEntity("/sync/status", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        return headers;
    }
}
