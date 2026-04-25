package com.catalogcollector.dto;

import com.catalogcollector.entity.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record EffectiveCatalogItemResponse(
        UUID id,
        String barcode,
        String barcodeType,
        String title,
        String publisher,
        Integer pageCount,
        String coverImageUrl,
        String edition,
        String language,
        String releaseDate,
        MediaType mediaType,
        Map<String, Object> metadata,
        String lookupSource,
        Instant lastEnrichedAt,
        Set<String> overriddenFields,
        Instant createdAt,
        Instant updatedAt
) {
}
