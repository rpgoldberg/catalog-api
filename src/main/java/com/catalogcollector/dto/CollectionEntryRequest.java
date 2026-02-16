package com.catalogcollector.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CollectionEntryRequest(
        @NotNull UUID catalogItemId,
        String condition,
        BigDecimal purchasePrice,
        LocalDate purchaseDate,
        String notes,
        @Min(1) Integer quantity
) {
}
