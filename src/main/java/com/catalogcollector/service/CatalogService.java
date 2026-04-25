package com.catalogcollector.service;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.CatalogItemRequest;
import com.catalogcollector.dto.CatalogItemResponse;
import com.catalogcollector.dto.CursorPage;
import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.repository.CatalogItemRepository;
import com.catalogcollector.service.CatalogEnrichmentService.ProviderLookupResult;
import com.catalogcollector.service.CursorPaginationHelper.DecodedCursor;
import com.catalogcollector.util.BarcodeClassifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CatalogService {

    private final CatalogItemRepository catalogItemRepository;
    private final CatalogEnrichmentService enrichmentService;

    public CatalogService(CatalogItemRepository catalogItemRepository,
                          CatalogEnrichmentService enrichmentService) {
        this.catalogItemRepository = catalogItemRepository;
        this.enrichmentService = enrichmentService;
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getAllItems() {
        return catalogItemRepository.findAll().stream()
                .map(CatalogItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CursorPage<CatalogItemResponse> getItems(String cursor, int size) {
        List<CatalogItem> items;
        if (cursor == null || cursor.isEmpty()) {
            items = catalogItemRepository.findFirstPage(PageRequest.of(0, size + 1));
        } else {
            DecodedCursor decoded = CursorPaginationHelper.decode(cursor);
            items = catalogItemRepository.findAfterCursor(
                    decoded.timestamp(), decoded.id(), PageRequest.of(0, size + 1));
        }

        boolean hasMore = items.size() > size;
        List<CatalogItem> pageItems = hasMore ? items.subList(0, size) : items;
        String nextCursor = null;
        if (hasMore && !pageItems.isEmpty()) {
            CatalogItem last = pageItems.getLast();
            nextCursor = CursorPaginationHelper.encode(last.getCreatedAt(), last.getId());
        }

        List<CatalogItemResponse> content = pageItems.stream()
                .map(CatalogItemResponse::from)
                .toList();
        return new CursorPage<>(content, nextCursor, hasMore, content.size());
    }

    @Transactional(readOnly = true)
    public CatalogItemResponse getItemById(UUID id) {
        CatalogItem item = catalogItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found: " + id));
        return CatalogItemResponse.from(item);
    }

    @Cacheable(value = "barcode-lookups", key = "#barcode")
    @Transactional
    public CatalogItemResponse lookupByBarcode(String barcode) {
        Optional<CatalogItem> existing = catalogItemRepository.findByBarcode(barcode);
        if (existing.isPresent()) {
            return CatalogItemResponse.from(existing.get());
        }

        String barcodeType = BarcodeClassifier.classify(barcode);
        Optional<ProviderLookupResult> providerResult =
                enrichmentService.lookupBarcodeFromProviders(barcode, barcodeType);

        if (providerResult.isEmpty()) {
            throw new ResourceNotFoundException("No item found for barcode: " + barcode);
        }

        ProviderLookupResult lookup = providerResult.get();
        CatalogItem item = new CatalogItem(barcode, barcodeType, lookup.data().title());
        item.setCanonicalPublisher(lookup.data().publisher());
        item.setCanonicalPageCount(lookup.data().pageCount());
        item.setCanonicalCoverImageUrl(lookup.data().coverImageUrl());
        item.setCanonicalEdition(lookup.data().edition());
        item.setCanonicalLanguage(lookup.data().language());
        item.setCanonicalReleaseDate(lookup.data().releaseDate());
        item.setCanonicalMetadata(lookup.data().metadata());
        item.setLookupSource(lookup.sourceName());

        item = catalogItemRepository.save(item);
        enrichmentService.enrichItem(item);
        return CatalogItemResponse.from(item);
    }

    @Transactional
    public CatalogItemResponse createItem(CatalogItemRequest request) {
        String barcodeType = request.barcodeType();
        if ("UNKNOWN".equalsIgnoreCase(barcodeType) || barcodeType == null || barcodeType.isBlank()) {
            barcodeType = BarcodeClassifier.classify(request.barcode());
        }

        CatalogItem item = new CatalogItem(request.barcode(), barcodeType,
                request.canonicalTitle());
        item.setCanonicalPublisher(request.canonicalPublisher());
        item.setCanonicalPageCount(request.canonicalPageCount());
        item.setCanonicalCoverImageUrl(request.canonicalCoverImageUrl());
        item.setCanonicalEdition(request.canonicalEdition());
        item.setCanonicalLanguage(request.canonicalLanguage());
        item.setCanonicalReleaseDate(request.canonicalReleaseDate());
        item.setMediaType(request.mediaType());
        item.setCanonicalMetadata(request.canonicalMetadata());
        item.setClientId(request.clientId());

        item = catalogItemRepository.save(item);
        return CatalogItemResponse.from(item);
    }
}
