package com.catalogcollector.service;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.dto.EffectiveCatalogItemResponse;
import com.catalogcollector.dto.EnrichmentResponse;
import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.entity.MediaType;
import com.catalogcollector.entity.OverridePolicy;
import com.catalogcollector.entity.PendingUpdate;
import com.catalogcollector.entity.User;
import com.catalogcollector.entity.UserItemOverride;
import com.catalogcollector.repository.CatalogItemRepository;
import com.catalogcollector.repository.PendingUpdateRepository;
import com.catalogcollector.repository.UserItemOverrideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogEnrichmentServiceTest {

    @Mock
    private CatalogItemRepository catalogItemRepository;
    @Mock
    private UserItemOverrideRepository userItemOverrideRepository;
    @Mock
    private PendingUpdateRepository pendingUpdateRepository;
    @Mock
    private CatalogLookupProvider mockProvider;

    private CatalogEnrichmentService enrichmentService;

    private User testUser;
    private CatalogItem testItem;

    @BeforeEach
    void setUp() {
        enrichmentService = new CatalogEnrichmentService(
                catalogItemRepository, userItemOverrideRepository,
                pendingUpdateRepository, List.of(mockProvider), List.of());

        testUser = new User("user@example.com", "User", "hash");
        testUser.setId(UUID.randomUUID());

        testItem = new CatalogItem("9781234567890", "ISBN", null);
        testItem.setId(UUID.randomUUID());
        testItem.setMediaType(MediaType.MANGA);
    }

    // --- lookupBarcodeFromProviders ---

    @Test
    void lookupBarcodeFromProviders_shouldReturnFirstSuccessfulResult() {
        CatalogLookupResult result = new CatalogLookupResult(
                "Found Item", "Publisher", null, null, null, null, null, null);

        when(mockProvider.lookupByBarcode("850527003646", "UPC_A"))
                .thenReturn(Optional.of(result));
        when(mockProvider.getSourceName()).thenReturn("upcitemdb");

        Optional<CatalogEnrichmentService.ProviderLookupResult> lookup =
                enrichmentService.lookupBarcodeFromProviders("850527003646", "UPC_A");

        assertThat(lookup).isPresent();
        assertThat(lookup.get().data().title()).isEqualTo("Found Item");
        assertThat(lookup.get().sourceName()).isEqualTo("upcitemdb");
    }

    @Test
    void lookupBarcodeFromProviders_shouldReturnEmptyWhenNoProviderMatches() {
        when(mockProvider.lookupByBarcode("000000000000", "UPC_A"))
                .thenReturn(Optional.empty());

        Optional<CatalogEnrichmentService.ProviderLookupResult> lookup =
                enrichmentService.lookupBarcodeFromProviders("000000000000", "UPC_A");

        assertThat(lookup).isEmpty();
    }

    @Test
    void lookupBarcodeFromProviders_shouldIncludeSourceName() {
        CatalogLookupResult result = new CatalogLookupResult(
                "Title", null, null, null, null, null, null, null);

        when(mockProvider.lookupByBarcode("9781234567890", "ISBN_13"))
                .thenReturn(Optional.of(result));
        when(mockProvider.getSourceName()).thenReturn("openlibrary");

        Optional<CatalogEnrichmentService.ProviderLookupResult> lookup =
                enrichmentService.lookupBarcodeFromProviders("9781234567890", "ISBN_13");

        assertThat(lookup).isPresent();
        assertThat(lookup.get().sourceName()).isEqualTo("openlibrary");
    }

    // --- enrichItem (initial enrichment) ---

    @Test
    void enrichItem_shouldPopulateCanonicalFieldsFromProvider() {
        CatalogLookupResult result = new CatalogLookupResult(
                "Manga Title", "Publisher Co", 200,
                "https://covers.example.com/cover.jpg",
                "1st Edition", "ja", "2025-01-15",
                Map.of("volumes", 5));

        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockProvider.lookupByBarcode("9781234567890", "ISBN"))
                .thenReturn(Optional.of(result));
        when(mockProvider.getSourceName()).thenReturn("openlibrary");
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        enrichmentService.enrichItem(testItem);

        assertThat(testItem.getCanonicalTitle()).isEqualTo("Manga Title");
        assertThat(testItem.getCanonicalPublisher()).isEqualTo("Publisher Co");
        assertThat(testItem.getCanonicalPageCount()).isEqualTo(200);
        assertThat(testItem.getCanonicalCoverImageUrl())
                .isEqualTo("https://covers.example.com/cover.jpg");
        assertThat(testItem.getCanonicalEdition()).isEqualTo("1st Edition");
        assertThat(testItem.getCanonicalLanguage()).isEqualTo("ja");
        assertThat(testItem.getCanonicalReleaseDate()).isEqualTo("2025-01-15");
        assertThat(testItem.getLookupSource()).isEqualTo("openlibrary");
        assertThat(testItem.getLastEnrichedAt()).isNotNull();
        verify(catalogItemRepository).save(testItem);
    }

    @Test
    void enrichItem_shouldSkipWhenNoProviderMatches() {
        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.GAME));

        enrichmentService.enrichItem(testItem);

        assertThat(testItem.getCanonicalTitle()).isNull();
        verify(catalogItemRepository, never()).save(any());
    }

    @Test
    void enrichItem_shouldSkipWhenProviderReturnsEmpty() {
        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockProvider.lookupByBarcode("9781234567890", "ISBN"))
                .thenReturn(Optional.empty());

        enrichmentService.enrichItem(testItem);

        assertThat(testItem.getCanonicalTitle()).isNull();
        verify(catalogItemRepository, never()).save(any());
    }

    // --- reEnrichItem (re-enrichment with override policies) ---

    @Test
    void reEnrichItem_shouldUpdateCanonicalAndReturnChanges() {
        testItem.setCanonicalTitle("Old Title");
        testItem.setCanonicalPublisher("Old Publisher");

        CatalogLookupResult result = new CatalogLookupResult(
                "New Title", "New Publisher", null, null, null, null, null, null);

        when(catalogItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockProvider.lookupByBarcode("9781234567890", "ISBN"))
                .thenReturn(Optional.of(result));
        when(mockProvider.getSourceName()).thenReturn("openlibrary");
        when(userItemOverrideRepository.findByCatalogItemIdAndFieldName(any(), any()))
                .thenReturn(List.of());
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        EnrichmentResponse response = enrichmentService.reEnrichItem(testItem.getId());

        assertThat(response.changes()).hasSize(2);
        assertThat(response.changes().stream().map(EnrichmentResponse.FieldChange::fieldName))
                .containsExactlyInAnyOrder("title", "publisher");
        assertThat(testItem.getCanonicalTitle()).isEqualTo("New Title");
        assertThat(testItem.getCanonicalPublisher()).isEqualTo("New Publisher");
    }

    @Test
    void reEnrichItem_shouldThrowWhenItemNotFound() {
        UUID missingId = UUID.randomUUID();
        when(catalogItemRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrichmentService.reEnrichItem(missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reEnrichItem_shouldCreatePendingUpdateForReviewPolicy() {
        testItem.setCanonicalTitle("Old Title");

        CatalogLookupResult result = new CatalogLookupResult(
                "New Title", null, null, null, null, null, null, null);

        UserItemOverride override = new UserItemOverride(
                testUser, testItem, "title", "\"User Title\"", OverridePolicy.REVIEW);
        override.setId(UUID.randomUUID());

        when(catalogItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockProvider.lookupByBarcode("9781234567890", "ISBN"))
                .thenReturn(Optional.of(result));
        when(mockProvider.getSourceName()).thenReturn("openlibrary");
        when(userItemOverrideRepository.findByCatalogItemIdAndFieldName(testItem.getId(), "title"))
                .thenReturn(List.of(override));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));
        when(pendingUpdateRepository.save(any(PendingUpdate.class)))
                .thenAnswer(i -> i.getArgument(0));

        EnrichmentResponse response = enrichmentService.reEnrichItem(testItem.getId());

        assertThat(response.pendingReviewsCreated()).isEqualTo(1);
        verify(pendingUpdateRepository).save(any(PendingUpdate.class));
    }

    @Test
    void reEnrichItem_shouldDeleteOverrideForAutoAcceptPolicy() {
        testItem.setCanonicalTitle("Old Title");

        CatalogLookupResult result = new CatalogLookupResult(
                "New Title", null, null, null, null, null, null, null);

        UserItemOverride override = new UserItemOverride(
                testUser, testItem, "title", "\"User Title\"", OverridePolicy.AUTO_ACCEPT);
        override.setId(UUID.randomUUID());

        when(catalogItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockProvider.lookupByBarcode("9781234567890", "ISBN"))
                .thenReturn(Optional.of(result));
        when(mockProvider.getSourceName()).thenReturn("openlibrary");
        when(userItemOverrideRepository.findByCatalogItemIdAndFieldName(testItem.getId(), "title"))
                .thenReturn(List.of(override));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        enrichmentService.reEnrichItem(testItem.getId());

        verify(userItemOverrideRepository).delete(override);
    }

    @Test
    void reEnrichItem_shouldNotTouchLockedOverride() {
        testItem.setCanonicalTitle("Old Title");

        CatalogLookupResult result = new CatalogLookupResult(
                "New Title", null, null, null, null, null, null, null);

        UserItemOverride override = new UserItemOverride(
                testUser, testItem, "title", "\"Locked Title\"", OverridePolicy.LOCKED);
        override.setId(UUID.randomUUID());

        when(catalogItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockProvider.lookupByBarcode("9781234567890", "ISBN"))
                .thenReturn(Optional.of(result));
        when(mockProvider.getSourceName()).thenReturn("openlibrary");
        when(userItemOverrideRepository.findByCatalogItemIdAndFieldName(testItem.getId(), "title"))
                .thenReturn(List.of(override));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        enrichmentService.reEnrichItem(testItem.getId());

        verify(userItemOverrideRepository, never()).delete(any());
        verify(pendingUpdateRepository, never()).save(any());
    }

    // --- getEffectiveView ---

    @Test
    void getEffectiveView_shouldReturnCanonicalWhenNoOverrides() {
        testItem.setCanonicalTitle("Canonical Title");
        testItem.setCanonicalPublisher("Canonical Publisher");

        when(catalogItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(userItemOverrideRepository.findByUserIdAndCatalogItemId(
                testUser.getId(), testItem.getId())).thenReturn(List.of());

        EffectiveCatalogItemResponse response =
                enrichmentService.getEffectiveView(testUser.getId(), testItem.getId());

        assertThat(response.title()).isEqualTo("Canonical Title");
        assertThat(response.publisher()).isEqualTo("Canonical Publisher");
        assertThat(response.overriddenFields()).isEmpty();
    }

    @Test
    void getEffectiveView_shouldMergeUserOverrides() {
        testItem.setCanonicalTitle("Canonical Title");
        testItem.setCanonicalPublisher("Canonical Publisher");

        UserItemOverride titleOverride = new UserItemOverride(
                testUser, testItem, "title", "\"My Custom Title\"", OverridePolicy.LOCKED);

        when(catalogItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(userItemOverrideRepository.findByUserIdAndCatalogItemId(
                testUser.getId(), testItem.getId())).thenReturn(List.of(titleOverride));

        EffectiveCatalogItemResponse response =
                enrichmentService.getEffectiveView(testUser.getId(), testItem.getId());

        assertThat(response.title()).isEqualTo("My Custom Title");
        assertThat(response.publisher()).isEqualTo("Canonical Publisher");
        assertThat(response.overriddenFields()).containsExactly("title");
    }

    @Test
    void getEffectiveView_shouldThrowWhenItemNotFound() {
        UUID missingId = UUID.randomUUID();
        when(catalogItemRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrichmentService.getEffectiveView(testUser.getId(), missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- no providers ---

    @Test
    void enrichItem_shouldHandleEmptyProviderList() {
        CatalogEnrichmentService emptyService = new CatalogEnrichmentService(
                catalogItemRepository, userItemOverrideRepository,
                pendingUpdateRepository, List.of(), List.of());

        emptyService.enrichItem(testItem);

        verify(catalogItemRepository, never()).save(any());
    }

    // --- secondary enrichment ---

    @Test
    void enrichItem_shouldApplySecondaryEnrichmentAfterPrimary() {
        SecondaryEnricher mockEnricher = org.mockito.Mockito.mock(SecondaryEnricher.class);
        CatalogEnrichmentService serviceWithEnricher = new CatalogEnrichmentService(
                catalogItemRepository, userItemOverrideRepository,
                pendingUpdateRepository, List.of(mockProvider), List.of(mockEnricher));

        CatalogLookupResult primaryResult = new CatalogLookupResult(
                "Movie Title", "Studio", null,
                "https://primary.com/cover.jpg",
                null, null, null, Map.of("source", "primary"));

        CatalogLookupResult secondaryResult = new CatalogLookupResult(
                null, null, null,
                "https://tmdb.org/better-cover.jpg",
                null, null, "2008-07-16",
                Map.of("synopsis", "A great movie", "rating", 8.5));

        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockProvider.lookupByBarcode("9781234567890", "ISBN"))
                .thenReturn(Optional.of(primaryResult));
        when(mockProvider.getSourceName()).thenReturn("primary");
        when(mockEnricher.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockEnricher.enrichByTitle("Movie Title", MediaType.MANGA))
                .thenReturn(Optional.of(secondaryResult));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        serviceWithEnricher.enrichItem(testItem);

        // Secondary should overwrite cover image
        assertThat(testItem.getCanonicalCoverImageUrl()).isEqualTo("https://tmdb.org/better-cover.jpg");
        // Secondary should fill missing releaseDate
        assertThat(testItem.getCanonicalReleaseDate()).isEqualTo("2008-07-16");
        // Secondary metadata should merge with primary
        assertThat(testItem.getCanonicalMetadata()).containsEntry("source", "primary");
        assertThat(testItem.getCanonicalMetadata()).containsEntry("synopsis", "A great movie");
        assertThat(testItem.getCanonicalMetadata()).containsEntry("rating", 8.5);
    }

    @Test
    void enrichItem_shouldSkipSecondaryWhenNoTitleFromPrimary() {
        SecondaryEnricher mockEnricher = org.mockito.Mockito.mock(SecondaryEnricher.class);
        CatalogEnrichmentService serviceWithEnricher = new CatalogEnrichmentService(
                catalogItemRepository, userItemOverrideRepository,
                pendingUpdateRepository, List.of(mockProvider), List.of(mockEnricher));

        CatalogLookupResult primaryResult = new CatalogLookupResult(
                null, "Publisher", null, "https://img.com/cover.jpg",
                null, null, null, null);

        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockProvider.lookupByBarcode("9781234567890", "ISBN"))
                .thenReturn(Optional.of(primaryResult));
        when(mockProvider.getSourceName()).thenReturn("primary");
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        serviceWithEnricher.enrichItem(testItem);

        verify(mockEnricher, never()).enrichByTitle(any(), any());
    }

    @Test
    void enrichItem_shouldSkipSecondaryWhenMediaTypeDoesNotMatch() {
        SecondaryEnricher mockEnricher = org.mockito.Mockito.mock(SecondaryEnricher.class);
        CatalogEnrichmentService serviceWithEnricher = new CatalogEnrichmentService(
                catalogItemRepository, userItemOverrideRepository,
                pendingUpdateRepository, List.of(mockProvider), List.of(mockEnricher));

        CatalogLookupResult primaryResult = new CatalogLookupResult(
                "Some Title", null, null, null, null, null, null, null);

        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockProvider.lookupByBarcode("9781234567890", "ISBN"))
                .thenReturn(Optional.of(primaryResult));
        when(mockProvider.getSourceName()).thenReturn("primary");
        when(mockEnricher.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.GAME));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        serviceWithEnricher.enrichItem(testItem);

        verify(mockEnricher, never()).enrichByTitle(any(), any());
    }

    @Test
    void enrichItem_shouldHandleSecondaryReturningEmpty() {
        SecondaryEnricher mockEnricher = org.mockito.Mockito.mock(SecondaryEnricher.class);
        CatalogEnrichmentService serviceWithEnricher = new CatalogEnrichmentService(
                catalogItemRepository, userItemOverrideRepository,
                pendingUpdateRepository, List.of(mockProvider), List.of(mockEnricher));

        CatalogLookupResult primaryResult = new CatalogLookupResult(
                "Title", "Publisher", null,
                "https://primary.com/cover.jpg",
                null, null, "2020-01-01", null);

        when(mockProvider.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockProvider.lookupByBarcode("9781234567890", "ISBN"))
                .thenReturn(Optional.of(primaryResult));
        when(mockProvider.getSourceName()).thenReturn("primary");
        when(mockEnricher.getSupportedMediaTypes()).thenReturn(Set.of(MediaType.MANGA));
        when(mockEnricher.enrichByTitle("Title", MediaType.MANGA))
                .thenReturn(Optional.empty());
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        serviceWithEnricher.enrichItem(testItem);

        // Primary values should remain unchanged
        assertThat(testItem.getCanonicalTitle()).isEqualTo("Title");
        assertThat(testItem.getCanonicalCoverImageUrl()).isEqualTo("https://primary.com/cover.jpg");
        assertThat(testItem.getCanonicalReleaseDate()).isEqualTo("2020-01-01");
        verify(catalogItemRepository).save(testItem);
    }
}
