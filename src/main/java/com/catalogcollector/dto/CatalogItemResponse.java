package com.catalogcollector.dto;

import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.entity.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CatalogItemResponse(
        UUID id,
        String barcode,
        String barcodeType,
        String canonicalTitle,
        String canonicalPublisher,
        Integer canonicalPageCount,
        String canonicalCoverImageUrl,
        String canonicalEdition,
        String canonicalLanguage,
        String canonicalReleaseDate,
        String lookupSource,
        Instant lastEnrichedAt,
        MediaType mediaType,
        Map<String, Object> canonicalMetadata,
        String clientId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public static CatalogItemResponse from(CatalogItem item) {
        return new CatalogItemResponse(
                item.getId(),
                item.getBarcode(),
                item.getBarcodeType(),
                item.getCanonicalTitle(),
                item.getCanonicalPublisher(),
                item.getCanonicalPageCount(),
                item.getCanonicalCoverImageUrl(),
                item.getCanonicalEdition(),
                item.getCanonicalLanguage(),
                item.getCanonicalReleaseDate(),
                item.getLookupSource(),
                item.getLastEnrichedAt(),
                item.getMediaType(),
                item.getCanonicalMetadata(),
                item.getClientId(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getDeletedAt());
    }
}
