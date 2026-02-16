package com.catalogcollector.dto;

import com.catalogcollector.entity.CollectionEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CollectionEntryResponse(
        UUID id,
        UUID userId,
        UUID catalogItemId,
        String catalogItemName,
        String condition,
        BigDecimal purchasePrice,
        LocalDate purchaseDate,
        String notes,
        Integer quantity,
        Instant createdAt,
        Instant updatedAt
) {
    public static CollectionEntryResponse from(CollectionEntry entry) {
        return new CollectionEntryResponse(
                entry.getId(),
                entry.getUser().getId(),
                entry.getCatalogItem().getId(),
                entry.getCatalogItem().getName(),
                entry.getCondition(),
                entry.getPurchasePrice(),
                entry.getPurchaseDate(),
                entry.getNotes(),
                entry.getQuantity(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }
}
