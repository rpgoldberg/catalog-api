package com.catalogcollector.service;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.CatalogItemRequest;
import com.catalogcollector.dto.CatalogItemResponse;
import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.dto.CursorPage;
import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.entity.MediaType;
import com.catalogcollector.repository.CatalogItemRepository;
import com.catalogcollector.service.CatalogEnrichmentService.ProviderLookupResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CatalogItemRepository catalogItemRepository;

    @Mock
    private CatalogEnrichmentService enrichmentService;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(catalogItemRepository, enrichmentService);
    }

    @Test
    void getAllItems_shouldReturnAllItems() {
        CatalogItem item = createTestItem();
        when(catalogItemRepository.findAll()).thenReturn(List.of(item));

        List<CatalogItemResponse> results = catalogService.getAllItems();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().barcode()).isEqualTo("1234567890");
    }

    @Test
    void getItemById_shouldReturnItem() {
        CatalogItem item = createTestItem();
        when(catalogItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        CatalogItemResponse response = catalogService.getItemById(item.getId());

        assertThat(response.canonicalTitle()).isEqualTo("Test Figure");
    }

    @Test
    void getItemById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(catalogItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getItemById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void lookupByBarcode_shouldReturnItemForKnownBarcode() {
        CatalogItem item = createTestItem();
        when(catalogItemRepository.findByBarcode("1234567890")).thenReturn(Optional.of(item));

        CatalogItemResponse response = catalogService.lookupByBarcode("1234567890");

        assertThat(response.barcode()).isEqualTo("1234567890");
    }

    @Test
    void lookupByBarcode_shouldThrowForUnknownBarcode() {
        when(catalogItemRepository.findByBarcode("unknown")).thenReturn(Optional.empty());
        when(enrichmentService.lookupBarcodeFromProviders("unknown", "UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.lookupByBarcode("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void lookupByBarcode_shouldFallBackToProviders_whenNotInDb() {
        when(catalogItemRepository.findByBarcode("850527003646")).thenReturn(Optional.empty());

        CatalogLookupResult lookupData = new CatalogLookupResult(
                "Koimonogatari Blu-ray", "Aniplex", null,
                "https://img.example.com/koimono.jpg",
                null, "ja", "2015-06-24", null);
        ProviderLookupResult providerResult = new ProviderLookupResult(lookupData, "upcitemdb");

        when(enrichmentService.lookupBarcodeFromProviders("850527003646", "UPC_A"))
                .thenReturn(Optional.of(providerResult));

        CatalogItem saved = new CatalogItem("850527003646", "UPC_A", "Koimonogatari Blu-ray");
        saved.setId(UUID.randomUUID());
        saved.setCanonicalPublisher("Aniplex");
        saved.setCanonicalCoverImageUrl("https://img.example.com/koimono.jpg");
        saved.setCanonicalLanguage("ja");
        saved.setCanonicalReleaseDate("2015-06-24");
        saved.setLookupSource("upcitemdb");

        when(catalogItemRepository.save(any(CatalogItem.class))).thenReturn(saved);

        CatalogItemResponse response = catalogService.lookupByBarcode("850527003646");

        assertThat(response.canonicalTitle()).isEqualTo("Koimonogatari Blu-ray");
        assertThat(response.lookupSource()).isEqualTo("upcitemdb");
        assertThat(response.barcodeType()).isEqualTo("UPC_A");
        verify(catalogItemRepository).save(any(CatalogItem.class));
        verify(enrichmentService).enrichItem(any(CatalogItem.class));
    }

    @Test
    void lookupByBarcode_shouldClassifyBarcodeType() {
        when(catalogItemRepository.findByBarcode("9781506750088")).thenReturn(Optional.empty());

        CatalogLookupResult lookupData = new CatalogLookupResult(
                "Some Book", "Publisher", 300, null, null, "en", null, null);
        ProviderLookupResult providerResult = new ProviderLookupResult(lookupData, "openlibrary");

        when(enrichmentService.lookupBarcodeFromProviders("9781506750088", "ISBN_13"))
                .thenReturn(Optional.of(providerResult));

        CatalogItem saved = new CatalogItem("9781506750088", "ISBN_13", "Some Book");
        saved.setId(UUID.randomUUID());
        when(catalogItemRepository.save(any(CatalogItem.class))).thenReturn(saved);

        CatalogItemResponse response = catalogService.lookupByBarcode("9781506750088");

        assertThat(response.barcodeType()).isEqualTo("ISBN_13");
    }

    @Test
    void lookupByBarcode_shouldThrow404_whenBothDbAndProvidersEmpty() {
        when(catalogItemRepository.findByBarcode("4571306970084")).thenReturn(Optional.empty());
        when(enrichmentService.lookupBarcodeFromProviders("4571306970084", "JAN"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.lookupByBarcode("4571306970084"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("4571306970084");
    }

    @Test
    void createItem_shouldPersistAndReturn() {
        CatalogItemRequest request = new CatalogItemRequest(
                "9876543210", "EAN13", "New Item", "Publisher",
                null, null, null, null, null,
                MediaType.OTHER, Map.of("key", "value"), null);

        CatalogItem saved = new CatalogItem("9876543210", "EAN13", "New Item");
        saved.setId(UUID.randomUUID());
        saved.setCanonicalPublisher("Publisher");

        when(catalogItemRepository.save(any(CatalogItem.class))).thenReturn(saved);

        CatalogItemResponse response = catalogService.createItem(request);

        assertThat(response.barcode()).isEqualTo("9876543210");
        assertThat(response.canonicalTitle()).isEqualTo("New Item");
    }

    @Test
    void createItem_shouldSetAllCanonicalFields() {
        CatalogItemRequest request = new CatalogItemRequest(
                "9781234567890", "ISBN", "Manga Title", "Publisher Co",
                200, "https://img.example.com/cover.jpg", "1st Edition",
                "ja", "2025-01-15",
                MediaType.MANGA, Map.of("volumes", 5), "client-uuid-123");

        CatalogItem saved = new CatalogItem("9781234567890", "ISBN", "Manga Title");
        saved.setId(UUID.randomUUID());
        saved.setCanonicalPublisher("Publisher Co");
        saved.setCanonicalPageCount(200);
        saved.setCanonicalCoverImageUrl("https://img.example.com/cover.jpg");
        saved.setCanonicalEdition("1st Edition");
        saved.setCanonicalLanguage("ja");
        saved.setCanonicalReleaseDate("2025-01-15");
        saved.setMediaType(MediaType.MANGA);
        saved.setCanonicalMetadata(Map.of("volumes", 5));
        saved.setClientId("client-uuid-123");

        when(catalogItemRepository.save(any(CatalogItem.class))).thenReturn(saved);

        CatalogItemResponse response = catalogService.createItem(request);

        assertThat(response.canonicalTitle()).isEqualTo("Manga Title");
        assertThat(response.canonicalPublisher()).isEqualTo("Publisher Co");
        assertThat(response.canonicalPageCount()).isEqualTo(200);
        assertThat(response.canonicalCoverImageUrl()).isEqualTo("https://img.example.com/cover.jpg");
        assertThat(response.canonicalEdition()).isEqualTo("1st Edition");
        assertThat(response.canonicalLanguage()).isEqualTo("ja");
        assertThat(response.canonicalReleaseDate()).isEqualTo("2025-01-15");
        assertThat(response.mediaType()).isEqualTo(MediaType.MANGA);
        assertThat(response.clientId()).isEqualTo("client-uuid-123");
    }

    @Test
    void getItems_firstPage_shouldReturnPageWithCursor() {
        List<CatalogItem> items = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            CatalogItem item = new CatalogItem("barcode-" + i, "UPC", "Item " + i);
            item.setId(UUID.randomUUID());
            item.setCreatedAt(Instant.now().plusSeconds(i));
            items.add(item);
        }
        when(catalogItemRepository.findFirstPage(any(PageRequest.class))).thenReturn(items);

        CursorPage<CatalogItemResponse> page = catalogService.getItems(null, 2);

        assertThat(page.content()).hasSize(2);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isNotNull();
    }

    @Test
    void getItems_lastPage_shouldReturnPageWithoutCursor() {
        CatalogItem item = createTestItem();
        item.setCreatedAt(Instant.now());
        when(catalogItemRepository.findFirstPage(any(PageRequest.class)))
                .thenReturn(List.of(item));

        CursorPage<CatalogItemResponse> page = catalogService.getItems(null, 5);

        assertThat(page.content()).hasSize(1);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void getItems_withCursor_shouldQueryAfterCursor() {
        Instant ts = Instant.parse("2025-06-01T00:00:00Z");
        UUID id = UUID.randomUUID();
        String cursor = CursorPaginationHelper.encode(ts, id);

        CatalogItem item = createTestItem();
        item.setCreatedAt(Instant.now());
        when(catalogItemRepository.findAfterCursor(eq(ts), eq(id), any(PageRequest.class)))
                .thenReturn(List.of(item));

        CursorPage<CatalogItemResponse> page = catalogService.getItems(cursor, 10);

        assertThat(page.content()).hasSize(1);
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void createItem_shouldAutoClassifyBarcodeType_whenUnknown() {
        CatalogItemRequest request = new CatalogItemRequest(
                "9781234567890", "UNKNOWN", "My Book", null,
                null, null, null, null, null,
                MediaType.OTHER, null, null);

        CatalogItem saved = new CatalogItem("9781234567890", "ISBN_13", "My Book");
        saved.setId(UUID.randomUUID());
        when(catalogItemRepository.save(any(CatalogItem.class))).thenReturn(saved);

        catalogService.createItem(request);

        ArgumentCaptor<CatalogItem> captor = ArgumentCaptor.forClass(CatalogItem.class);
        verify(catalogItemRepository).save(captor.capture());
        assertThat(captor.getValue().getBarcodeType()).isEqualTo("ISBN_13");
    }

    @Test
    void createItem_shouldKeepBarcodeType_whenAlreadyKnown() {
        CatalogItemRequest request = new CatalogItemRequest(
                "9781234567890", "ISBN", "My Book", null,
                null, null, null, null, null,
                MediaType.MANGA, null, null);

        CatalogItem saved = new CatalogItem("9781234567890", "ISBN", "My Book");
        saved.setId(UUID.randomUUID());
        when(catalogItemRepository.save(any(CatalogItem.class))).thenReturn(saved);

        catalogService.createItem(request);

        ArgumentCaptor<CatalogItem> captor = ArgumentCaptor.forClass(CatalogItem.class);
        verify(catalogItemRepository).save(captor.capture());
        assertThat(captor.getValue().getBarcodeType()).isEqualTo("ISBN");
    }

    private CatalogItem createTestItem() {
        CatalogItem item = new CatalogItem("1234567890", "UPC", "Test Figure");
        item.setId(UUID.randomUUID());
        item.setCanonicalPublisher("TestBrand");
        item.setMediaType(MediaType.OTHER);
        return item;
    }
}
