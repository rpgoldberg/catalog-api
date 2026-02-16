package com.catalogcollector.dto;

import com.catalogcollector.entity.CatalogItem;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CatalogItemResponse(
        UUID id,
        String barcode,
        String barcodeType,
        String name,
        String brand,
        String category,
        String description,
        String imageUrl,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {
    public static CatalogItemResponse from(CatalogItem item) {
        return new CatalogItemResponse(
                item.getId(),
                item.getBarcode(),
                item.getBarcodeType(),
                item.getName(),
                item.getBrand(),
                item.getCategory(),
                item.getDescription(),
                item.getImageUrl(),
                item.getMetadata(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
