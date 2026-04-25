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
        String catalogItemTitle,
        String condition,
        BigDecimal purchasePrice,
        LocalDate purchaseDate,
        String notes,
        Integer quantity,
        String clientId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public static CollectionEntryResponse from(CollectionEntry entry) {
        return new CollectionEntryResponse(
                entry.getId(),
                entry.getUser().getId(),
                entry.getCatalogItem().getId(),
                entry.getCatalogItem().getCanonicalTitle(),
                entry.getCondition(),
                entry.getPurchasePrice(),
                entry.getPurchaseDate(),
                entry.getNotes(),
                entry.getQuantity(),
                entry.getClientId(),
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                entry.getDeletedAt());
    }
}
