package com.catalogcollector.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CatalogItemRequest(
        @NotBlank String barcode,
        @NotBlank String barcodeType,
        @NotBlank String name,
        String brand,
        String category,
        String description,
        String imageUrl,
        Map<String, Object> metadata
) {
}
