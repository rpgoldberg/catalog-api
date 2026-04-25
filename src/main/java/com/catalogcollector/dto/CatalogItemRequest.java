package com.catalogcollector.dto;

import com.catalogcollector.entity.MediaType;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CatalogItemRequest(
        @NotBlank String barcode,
        @NotBlank String barcodeType,
        String canonicalTitle,
        String canonicalPublisher,
        Integer canonicalPageCount,
        String canonicalCoverImageUrl,
        String canonicalEdition,
        String canonicalLanguage,
        String canonicalReleaseDate,
        MediaType mediaType,
        Map<String, Object> canonicalMetadata,
        String clientId
) {
}
