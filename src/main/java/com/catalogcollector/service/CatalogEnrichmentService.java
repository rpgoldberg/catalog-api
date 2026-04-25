package com.catalogcollector.service;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.dto.EffectiveCatalogItemResponse;
import com.catalogcollector.dto.EnrichmentResponse;
import com.catalogcollector.dto.EnrichmentResponse.FieldChange;
import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.entity.OverridePolicy;
import com.catalogcollector.entity.PendingUpdate;
import com.catalogcollector.entity.UserItemOverride;
import com.catalogcollector.repository.CatalogItemRepository;
import com.catalogcollector.repository.PendingUpdateRepository;
import com.catalogcollector.repository.UserItemOverrideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CatalogEnrichmentService {

    private final CatalogItemRepository catalogItemRepository;
    private final UserItemOverrideRepository userItemOverrideRepository;
    private final PendingUpdateRepository pendingUpdateRepository;
    private final List<CatalogLookupProvider> providers;
    private final List<SecondaryEnricher> secondaryEnrichers;

    public CatalogEnrichmentService(CatalogItemRepository catalogItemRepository,
                                    UserItemOverrideRepository userItemOverrideRepository,
                                    PendingUpdateRepository pendingUpdateRepository,
                                    List<CatalogLookupProvider> providers,
                                    List<SecondaryEnricher> secondaryEnrichers) {
        this.catalogItemRepository = catalogItemRepository;
        this.userItemOverrideRepository = userItemOverrideRepository;
        this.pendingUpdateRepository = pendingUpdateRepository;
        this.providers = providers;
        this.secondaryEnrichers = secondaryEnrichers;
    }

    public record ProviderLookupResult(CatalogLookupResult data, String sourceName) {}

    public Optional<ProviderLookupResult> lookupBarcodeFromProviders(String barcode, String barcodeType) {
        for (CatalogLookupProvider provider : providers) {
            Optional<CatalogLookupResult> result = provider.lookupByBarcode(barcode, barcodeType);
            if (result.isPresent()) {
                return Optional.of(new ProviderLookupResult(result.get(), provider.getSourceName()));
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void enrichItem(CatalogItem item) {
        Optional<CatalogLookupResult> result = lookupBarcode(item);
        if (result.isEmpty()) {
            return;
        }
        applyLookupResult(item, result.get());
        applySecondaryEnrichment(item);
        catalogItemRepository.save(item);
    }

    @Transactional
    public EnrichmentResponse reEnrichItem(UUID itemId) {
        CatalogItem item = catalogItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found: " + itemId));

        Optional<CatalogLookupResult> lookupResult = lookupBarcode(item);
        if (lookupResult.isEmpty()) {
            return new EnrichmentResponse(itemId, null, List.of(), 0);
        }

        CatalogLookupResult result = lookupResult.get();
        String source = findSourceName(item);
        List<FieldChange> changes = new ArrayList<>();
        int pendingReviewsCreated = 0;

        pendingReviewsCreated += processFieldChange(item, "title",
                item.getCanonicalTitle(), result.title(), changes, source);
        pendingReviewsCreated += processFieldChange(item, "publisher",
                item.getCanonicalPublisher(), result.publisher(), changes, source);
        pendingReviewsCreated += processFieldChange(item, "pageCount",
                intToString(item.getCanonicalPageCount()),
                intToString(result.pageCount()), changes, source);
        pendingReviewsCreated += processFieldChange(item, "coverImageUrl",
                item.getCanonicalCoverImageUrl(), result.coverImageUrl(), changes, source);
        pendingReviewsCreated += processFieldChange(item, "edition",
                item.getCanonicalEdition(), result.edition(), changes, source);
        pendingReviewsCreated += processFieldChange(item, "language",
                item.getCanonicalLanguage(), result.language(), changes, source);
        pendingReviewsCreated += processFieldChange(item, "releaseDate",
                item.getCanonicalReleaseDate(), result.releaseDate(), changes, source);

        if (!changes.isEmpty()) {
            applyLookupResult(item, result);
            applySecondaryEnrichment(item);
            catalogItemRepository.save(item);
        }

        return new EnrichmentResponse(itemId, source, changes, pendingReviewsCreated);
    }

    @Transactional(readOnly = true)
    public EffectiveCatalogItemResponse getEffectiveView(UUID userId, UUID itemId) {
        CatalogItem item = catalogItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found: " + itemId));

        List<UserItemOverride> overrides =
                userItemOverrideRepository.findByUserIdAndCatalogItemId(userId, itemId);

        Map<String, String> overrideMap = new java.util.HashMap<>();
        Set<String> overriddenFields = new HashSet<>();
        for (UserItemOverride override : overrides) {
            overrideMap.put(override.getFieldName(), override.getOverrideValue());
            overriddenFields.add(override.getFieldName());
        }

        return new EffectiveCatalogItemResponse(
                item.getId(),
                item.getBarcode(),
                item.getBarcodeType(),
                resolveField(overrideMap, "title", item.getCanonicalTitle()),
                resolveField(overrideMap, "publisher", item.getCanonicalPublisher()),
                resolveIntField(overrideMap, "pageCount", item.getCanonicalPageCount()),
                resolveField(overrideMap, "coverImageUrl", item.getCanonicalCoverImageUrl()),
                resolveField(overrideMap, "edition", item.getCanonicalEdition()),
                resolveField(overrideMap, "language", item.getCanonicalLanguage()),
                resolveField(overrideMap, "releaseDate", item.getCanonicalReleaseDate()),
                item.getMediaType(),
                item.getCanonicalMetadata(),
                item.getLookupSource(),
                item.getLastEnrichedAt(),
                overriddenFields,
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    private void applySecondaryEnrichment(CatalogItem item) {
        if (item.getCanonicalTitle() == null || item.getCanonicalTitle().isBlank()) {
            return;
        }
        for (SecondaryEnricher enricher : secondaryEnrichers) {
            if (item.getMediaType() != null
                    && !enricher.getSupportedMediaTypes().contains(item.getMediaType())) {
                continue;
            }
            Optional<CatalogLookupResult> secondary =
                    enricher.enrichByTitle(item.getCanonicalTitle(), item.getMediaType());
            if (secondary.isPresent()) {
                mergeSecondaryResult(item, secondary.get());
                return;
            }
        }
    }

    private void mergeSecondaryResult(CatalogItem item, CatalogLookupResult secondary) {
        if (secondary.coverImageUrl() != null) {
            item.setCanonicalCoverImageUrl(secondary.coverImageUrl());
        }
        if (item.getCanonicalReleaseDate() == null && secondary.releaseDate() != null) {
            item.setCanonicalReleaseDate(secondary.releaseDate());
        }
        if (secondary.metadata() != null) {
            Map<String, Object> existing = item.getCanonicalMetadata();
            if (existing == null) {
                item.setCanonicalMetadata(secondary.metadata());
            } else {
                Map<String, Object> merged = new java.util.HashMap<>(existing);
                merged.putAll(secondary.metadata());
                item.setCanonicalMetadata(merged);
            }
        }
    }

    private Optional<CatalogLookupResult> lookupBarcode(CatalogItem item) {
        for (CatalogLookupProvider provider : providers) {
            if (item.getMediaType() != null
                    && !provider.getSupportedMediaTypes().contains(item.getMediaType())) {
                continue;
            }
            Optional<CatalogLookupResult> result =
                    provider.lookupByBarcode(item.getBarcode(), item.getBarcodeType());
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private String findSourceName(CatalogItem item) {
        for (CatalogLookupProvider provider : providers) {
            if (item.getMediaType() != null
                    && !provider.getSupportedMediaTypes().contains(item.getMediaType())) {
                continue;
            }
            return provider.getSourceName();
        }
        return null;
    }

    private void applyLookupResult(CatalogItem item, CatalogLookupResult result) {
        if (result.title() != null) item.setCanonicalTitle(result.title());
        if (result.publisher() != null) item.setCanonicalPublisher(result.publisher());
        if (result.pageCount() != null) item.setCanonicalPageCount(result.pageCount());
        if (result.coverImageUrl() != null) item.setCanonicalCoverImageUrl(result.coverImageUrl());
        if (result.edition() != null) item.setCanonicalEdition(result.edition());
        if (result.language() != null) item.setCanonicalLanguage(result.language());
        if (result.releaseDate() != null) item.setCanonicalReleaseDate(result.releaseDate());
        if (result.metadata() != null) item.setCanonicalMetadata(result.metadata());

        item.setLookupSource(findSourceName(item));
        item.setLastEnrichedAt(Instant.now());
    }

    private int processFieldChange(CatalogItem item, String fieldName,
                                   String oldValue, String newValue,
                                   List<FieldChange> changes, String source) {
        if (newValue == null || Objects.equals(oldValue, newValue)) {
            return 0;
        }

        changes.add(new FieldChange(fieldName, oldValue, newValue));

        List<UserItemOverride> overrides =
                userItemOverrideRepository.findByCatalogItemIdAndFieldName(
                        item.getId(), fieldName);

        int pendingCreated = 0;
        for (UserItemOverride override : overrides) {
            switch (override.getPolicy()) {
                case AUTO_ACCEPT -> userItemOverrideRepository.delete(override);
                case REVIEW -> {
                    PendingUpdate pending = new PendingUpdate(
                            override.getUser(), item, fieldName, oldValue, newValue, source);
                    pendingUpdateRepository.save(pending);
                    pendingCreated++;
                }
                case LOCKED -> { /* do nothing */ }
            }
        }
        return pendingCreated;
    }

    private static String resolveField(Map<String, String> overrideMap,
                                       String fieldName, String canonical) {
        String override = overrideMap.get(fieldName);
        if (override != null) {
            return stripQuotes(override);
        }
        return canonical;
    }

    private static Integer resolveIntField(Map<String, String> overrideMap,
                                           String fieldName, Integer canonical) {
        String override = overrideMap.get(fieldName);
        if (override != null) {
            try {
                return Integer.parseInt(stripQuotes(override));
            } catch (NumberFormatException e) {
                return canonical;
            }
        }
        return canonical;
    }

    private static String stripQuotes(String value) {
        if (value != null && value.length() >= 2
                && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String intToString(Integer value) {
        return value != null ? value.toString() : null;
    }
}
